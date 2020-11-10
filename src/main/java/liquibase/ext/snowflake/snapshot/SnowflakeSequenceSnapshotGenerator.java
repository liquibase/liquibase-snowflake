package liquibase.ext.snowflake.snapshot;

import liquibase.database.Database;
import liquibase.ext.snowflake.database.SnowflakeDatabase;
import liquibase.snapshot.SnapshotGenerator;
import liquibase.snapshot.jvm.SequenceSnapshotGenerator;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Schema;

public class SnowflakeSequenceSnapshotGenerator extends SequenceSnapshotGenerator {

    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        int priority = super.getPriority(objectType, database);
        if (database instanceof SnowflakeDatabase) {
            priority += PRIORITY_DATABASE;
        }
        return priority;
    }

    @Override
    public Class<? extends SnapshotGenerator>[] replaces() {
        return new Class[] { SequenceSnapshotGenerator.class };
    }

    @Override
    protected String getSelectSequenceSql(Schema schema, Database database) {
        if (database instanceof SnowflakeDatabase) {
            return "SHOW SEQUENCES IN " + database.getDefaultCatalogName().toUpperCase() + "." + database.getDefaultSchemaName().toUpperCase();
        }

        return super.getSelectSequenceSql(schema, database);

    }

}