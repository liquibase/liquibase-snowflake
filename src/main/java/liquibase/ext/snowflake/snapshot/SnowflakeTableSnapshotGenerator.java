package liquibase.ext.snowflake.snapshot;

import liquibase.database.AbstractJdbcDatabase;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.ext.snowflake.database.SnowflakeDatabase;
import liquibase.snapshot.CachedRow;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.JdbcDatabaseSnapshot;
import liquibase.snapshot.jvm.TableSnapshotGenerator;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Table;

import java.sql.SQLException;
import java.util.List;

public class SnowflakeTableSnapshotGenerator extends TableSnapshotGenerator {
    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        int priority = super.getPriority(objectType, database);
        if (database instanceof SnowflakeDatabase) {
            priority += PRIORITY_DATABASE;
        }
        return priority;
    }
    @Override
    protected DatabaseObject snapshotObject(DatabaseObject example, DatabaseSnapshot snapshot) throws DatabaseException {
        Database database = snapshot.getDatabase();
        String objectName = example.getName();
        Schema schema = example.getSchema();

        List<CachedRow> rs = null;
        try {
            JdbcDatabaseSnapshot.CachingDatabaseMetaData metaData = ((JdbcDatabaseSnapshot) snapshot).getMetaDataFromCache();
            rs = metaData.getTables(((AbstractJdbcDatabase) database).getJdbcCatalogName(schema), ((AbstractJdbcDatabase) database).getJdbcSchemaName(schema), objectName.toUpperCase());

            Table table;
            if (!rs.isEmpty()) {
                table = readTable(rs.get(0), database);
            } else {
                return null;
            }

            return table;
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }
}
