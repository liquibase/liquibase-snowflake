package liquibase.ext.snowflake.snapshot;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import liquibase.Scope;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.executor.ExecutorService;
import liquibase.ext.snowflake.database.SnowflakeDatabase;
import liquibase.snapshot.CachedRow;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.SnapshotGenerator;
import liquibase.snapshot.jvm.UniqueConstraintSnapshotGenerator;
import liquibase.statement.core.RawSqlStatement;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Relation;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Table;
import liquibase.structure.core.UniqueConstraint;

public class SnowflakeUniqueConstraintSnapshotGenerator extends UniqueConstraintSnapshotGenerator {


    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (database instanceof SnowflakeDatabase) {
            return PRIORITY_DATABASE;
        } else {
            return PRIORITY_NONE;
        }
    }

    @Override
    public Class<? extends SnapshotGenerator>[] replaces() {
        return new Class[] { UniqueConstraintSnapshotGenerator.class };
    }

    @Override
    protected List<CachedRow> listConstraints(Table table, DatabaseSnapshot snapshot, Schema schema)
            throws DatabaseException, SQLException {
        return new SnowflakeResultSetConstraintsExtractor(snapshot, schema.getCatalogName(), schema.getName(), table.getName())
                .fastFetch();
    }

    @Override
    protected List<Map<String, ?>> listColumns(UniqueConstraint example, Database database, DatabaseSnapshot snapshot)
            throws DatabaseException {
        Relation table = example.getRelation();
        Schema schema = table.getSchema();
        String name = example.getName();
        String schemaName = database.correctObjectName(schema.getName(), Schema.class);
        String constraintName = database.correctObjectName(name, UniqueConstraint.class);
        String tableName = database.correctObjectName(table.getName(), Table.class);
        //TODO figure out what to do with COLUMN_NAME
        // https://community.snowflake.com/s/question/0D50Z00008Y3VUbSAN/table-details-including-the-constraints
        String sql = "select CONSTRAINT_NAME, CONSTRAINT_NAME as COLUMN_NAME " + "from " + database.getSystemSchema() +
                ".TABLE_CONSTRAINTS "
                + "where CONSTRAINT_TYPE='UNIQUE'";
        if (schemaName != null) {
            sql += "and CONSTRAINT_SCHEMA='" + schemaName + "' ";
        }
        if (tableName != null) {
            sql += "and TABLE_NAME='" + tableName + "' ";
        }
        if (constraintName != null) {
            sql += "and CONSTRAINT_NAME='" + constraintName + "'";
        }
        List<Map<String, ?>> rows = Scope.getCurrentScope().getSingleton(ExecutorService.class).getExecutor("jdbc", database)
                .queryForList(new RawSqlStatement(sql));

        return rows;
    }
}