package liquibase.ext.snowflake.snapshot;

import java.sql.SQLException;
import java.util.List;

import liquibase.CatalogAndSchema;
import liquibase.database.AbstractJdbcDatabase;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.snapshot.CachedRow;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.JdbcDatabaseSnapshot;
import liquibase.snapshot.SnowflakeResultSetCache;
import liquibase.snapshot.SnowflakeResultSetCache.SingleResultSetExtractor;
import liquibase.structure.core.Schema;

public class SnowflakeResultSetExtractor extends SingleResultSetExtractor {

    private DatabaseSnapshot databaseSnapshot;
    private Database database;
    private String catalogName;
    private String schemaName;
    private String tableName;

    public SnowflakeResultSetExtractor(DatabaseSnapshot databaseSnapshot, String catalogName, String schemaName,
                                  String tableName) {
        super(databaseSnapshot.getDatabase());
        this.databaseSnapshot = databaseSnapshot;
        this.database = databaseSnapshot.getDatabase();
        this.catalogName = catalogName;
        this.schemaName = schemaName;
        this.tableName = tableName;
    }

    @Override
    protected boolean shouldBulkSelect(String schemaKey, SnowflakeResultSetCache ResultSetCacheHana) {
        return this.tableName == null || getAllCatalogsStringScratchData() != null
                || super.shouldBulkSelect(schemaKey, ResultSetCacheHana);
    }

    @Override
    public boolean bulkContainsSchema(String schemaKey) {
        return false;
    }

    @Override
    public String getSchemaKey(CachedRow row) {
        return row.getString("CONSTRAINT_SCHEM");
    }

    @Override
    public SnowflakeResultSetCache.RowData rowKeyParameters(CachedRow row) {
        return new SnowflakeResultSetCache.RowData(this.catalogName, this.schemaName, this.database,
                row.getString("TABLE_NAME"));
    }

    @Override
    public SnowflakeResultSetCache.RowData wantedKeyParameters() {
        return new SnowflakeResultSetCache.RowData(this.catalogName, this.schemaName, this.database, this.tableName);
    }

    @Override
    public List<CachedRow> fastFetchQuery() throws SQLException, DatabaseException {
        CatalogAndSchema catalogAndSchema = new CatalogAndSchema(this.catalogName, this.schemaName)
                .customize(this.database);

        return executeAndExtract(
                createSql(((AbstractJdbcDatabase) this.database).getJdbcCatalogName(catalogAndSchema),
                        ((AbstractJdbcDatabase) this.database).getJdbcSchemaName(catalogAndSchema), this.tableName),
                this.database, false);
    }

    @Override
    public List<CachedRow> bulkFetchQuery() throws SQLException, DatabaseException {
        CatalogAndSchema catalogAndSchema = new CatalogAndSchema(this.catalogName, this.schemaName)
                .customize(this.database);

        return executeAndExtract(
                createSql(((AbstractJdbcDatabase) this.database).getJdbcCatalogName(catalogAndSchema),
                        ((AbstractJdbcDatabase) this.database).getJdbcSchemaName(catalogAndSchema), null),
                this.database);
    }

    private String createSql(String catalog, String schema, String table) {
        CatalogAndSchema catalogAndSchema = new CatalogAndSchema(catalog, schema).customize(this.database);

        String jdbcSchemaName = this.database.correctObjectName(
                ((AbstractJdbcDatabase) this.database).getJdbcSchemaName(catalogAndSchema), Schema.class);

        String sql = "select CONSTRAINT_NAME, CONSTRAINT_TYPE, TABLE_NAME from "
                + this.database.getSystemSchema() + ".TABLE_CONSTRAINTS " + "where TABLE_SCHEMA='" + jdbcSchemaName
                + "' and CONSTRAINT_TYPE='UNIQUE'";
        if (table != null) {
            sql += " and TABLE_NAME='" + table + "'";
        }

        return sql;
    }

    private String getAllCatalogsStringScratchData() {
        return (String) this.databaseSnapshot.getScratchData(JdbcDatabaseSnapshot.ALL_CATALOGS_STRING_SCRATCH_KEY);
    }

}