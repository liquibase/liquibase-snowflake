package liquibase.ext.snowflake.snapshot;

import liquibase.CatalogAndSchema;
import liquibase.database.AbstractJdbcDatabase;
import liquibase.database.Database;
import liquibase.database.core.Db2zDatabase;
import liquibase.diff.compare.DatabaseObjectComparatorFactory;
import liquibase.exception.DatabaseException;
import liquibase.snapshot.CachedRow;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.snapshot.JdbcDatabaseSnapshot;
import liquibase.snapshot.jvm.ForeignKeySnapshotGenerator;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.*;

import java.sql.DatabaseMetaData;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SnowflakeForeignKeySnapshotGenerator extends ForeignKeySnapshotGenerator {
    @Override
    protected void addTo(DatabaseObject foundObject, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {
        if (!snapshot.getSnapshotControl().shouldInclude(ForeignKey.class)) {
            return;
        }

        if (foundObject instanceof Table) {
            Table table = (Table) foundObject;
            Database database = snapshot.getDatabase();
            Schema schema;
            schema = table.getSchema();


            Set<String> seenFks = new HashSet<>();
            List<CachedRow> importedKeyMetadataResultSet;
            try {
                importedKeyMetadataResultSet = ((JdbcDatabaseSnapshot) snapshot).getMetaDataFromCache().getForeignKeys(((AbstractJdbcDatabase) database)
                                .getJdbcCatalogName(schema), ((AbstractJdbcDatabase) database).getJdbcSchemaName(schema),
                        table.getName(), null);

                for (CachedRow row : importedKeyMetadataResultSet) {
                    ForeignKey fk = new ForeignKey().setName(row.getString("FK_NAME")).setForeignKeyTable(table);
                    if (seenFks.add(fk.getName())) {
                        table.getOutgoingForeignKeys().add(fk);
                    }
                }
            } catch (Exception e) {
                throw new DatabaseException(e);
            }
        }
    }
    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot) throws DatabaseException, InvalidExampleException {

        Database database = snapshot.getDatabase();

        List<CachedRow> importedKeyMetadataResultSet;
        try {
            Table fkTable = ((ForeignKey) example).getForeignKeyTable();
            String searchCatalog = ((AbstractJdbcDatabase) database).getJdbcCatalogName(fkTable.getSchema());
            String searchSchema = ((AbstractJdbcDatabase) database).getJdbcSchemaName(fkTable.getSchema());
            String searchTableName = database.correctObjectName(fkTable.getName(), Table.class);

            importedKeyMetadataResultSet = ((JdbcDatabaseSnapshot) snapshot).getMetaDataFromCache().getForeignKeys(searchCatalog,
                    searchSchema, searchTableName.toUpperCase(), example.getName());
            ForeignKey foreignKey = null;
            for (CachedRow row : importedKeyMetadataResultSet) {
                String fk_name = cleanNameFromDatabase(row.getString("FK_NAME"), database);
                if (snapshot.getDatabase().isCaseSensitive()) {
                    if (!fk_name.equals(example.getName())) {
                        continue;
                    } else if (!fk_name.equalsIgnoreCase(example.getName())) {
                        continue;
                    }
                }

                if (foreignKey == null) {
                    foreignKey = new ForeignKey();
                }

                foreignKey.setName(fk_name);

                String fkTableCatalog = cleanNameFromDatabase(row.getString(METADATA_FKTABLE_CAT), database);
                String fkTableSchema = cleanNameFromDatabase(row.getString(METADATA_FKTABLE_SCHEM), database);
                String fkTableName = cleanNameFromDatabase(row.getString(METADATA_FKTABLE_NAME), database);
                Table foreignKeyTable = new Table().setName(fkTableName);
                foreignKeyTable.setSchema(new Schema(new Catalog(fkTableCatalog), fkTableSchema));

                foreignKey.setForeignKeyTable(foreignKeyTable);
                Column fkColumn = new Column(cleanNameFromDatabase(row.getString(METADATA_FKCOLUMN_NAME), database)).setRelation(foreignKeyTable);
                boolean alreadyAdded = false;
                for (Column existing : foreignKey.getForeignKeyColumns()) {
                    if (DatabaseObjectComparatorFactory.getInstance().isSameObject(existing, fkColumn, snapshot.getSchemaComparisons(), database)) {
                        alreadyAdded = true; //already added. One is probably an alias
                    }
                }
                if (alreadyAdded) {
                    break;
                }


                CatalogAndSchema pkTableSchema = ((AbstractJdbcDatabase) database).getSchemaFromJdbcInfo(
                        row.getString(METADATA_PKTABLE_CAT), row.getString(METADATA_PKTABLE_SCHEM));
                Table tempPkTable = (Table) new Table().setName(row.getString(METADATA_PKTABLE_NAME)).setSchema(
                        new Schema(pkTableSchema.getCatalogName(), pkTableSchema.getSchemaName()));
                foreignKey.setPrimaryKeyTable(tempPkTable);
                Column pkColumn = new Column(cleanNameFromDatabase(row.getString(METADATA_PKCOLUMN_NAME), database))
                        .setRelation(tempPkTable);

                foreignKey.addForeignKeyColumn(fkColumn);
                foreignKey.addPrimaryKeyColumn(pkColumn);
                //todo foreignKey.setKeySeq(importedKeyMetadataResultSet.getInt("KEY_SEQ"));

                // DB2 on z/OS doesn't support ON UPDATE
                if (!(database instanceof Db2zDatabase)) {
                    ForeignKeyConstraintType updateRule = convertToForeignKeyConstraintType(
                            row.getInt(METADATA_UPDATE_RULE), database);
                    foreignKey.setUpdateRule(updateRule);
                }
                ForeignKeyConstraintType deleteRule = convertToForeignKeyConstraintType(
                        row.getInt(METADATA_DELETE_RULE), database);
                foreignKey.setDeleteRule(deleteRule);

                short deferrability = row.getShort(METADATA_DEFERRABILITY);

                // Hsqldb doesn't handle setting this property correctly, it sets it to 0.
                // it should be set to DatabaseMetaData.importedKeyNotDeferrable(7)
                if ((deferrability == 0) || (deferrability == DatabaseMetaData.importedKeyNotDeferrable)) {
                    foreignKey.setDeferrable(false);
                    foreignKey.setInitiallyDeferred(false);
                } else if (deferrability == DatabaseMetaData.importedKeyInitiallyDeferred) {
                    foreignKey.setDeferrable(true);
                    foreignKey.setInitiallyDeferred(true);
                } else if (deferrability == DatabaseMetaData.importedKeyInitiallyImmediate) {
                    foreignKey.setDeferrable(true);
                    foreignKey.setInitiallyDeferred(false);
                } else {
                    throw new RuntimeException("Unknown deferrability result: " + deferrability);
                }

                Index exampleIndex = new Index().setRelation(foreignKey.getForeignKeyTable());
                exampleIndex.getColumns().addAll(foreignKey.getForeignKeyColumns());
                exampleIndex.addAssociatedWith(Index.MARK_FOREIGN_KEY);
                foreignKey.setBackingIndex(exampleIndex);

            }
            if (snapshot.get(ForeignKey.class).contains(foreignKey)) {
                return null;
            }
            return foreignKey;
        } catch (Exception e) {
            throw new DatabaseException(e);
        }
    }
}
