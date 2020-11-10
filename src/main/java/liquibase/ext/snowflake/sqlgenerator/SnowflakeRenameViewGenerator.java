package liquibase.ext.snowflake.sqlgenerator;
import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.ext.snowflake.database.SnowflakeDatabase;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.RenameViewGenerator;
import liquibase.statement.core.RenameTableStatement;
import liquibase.statement.core.RenameViewStatement;

public class SnowflakeRenameViewGenerator  extends RenameViewGenerator{
    @Override
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    @Override
    public boolean supports(RenameViewStatement statement, Database database) {
        return database instanceof SnowflakeDatabase;
    }

    @Override
    public Sql[] generateSql(RenameViewStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        return new Sql[] {
                new UnparsedSql(
                        "ALTER VIEW "
                                + database.escapeTableName(statement.getCatalogName(), statement.getSchemaName(),
                                statement.getOldViewName())
                                + " RENAME TO "
                                + database.escapeTableName(statement.getCatalogName(), statement.getSchemaName(),
                                statement.getNewViewName()),
                        getAffectedOldView(statement), getAffectedNewView(statement)) };
    }

}
