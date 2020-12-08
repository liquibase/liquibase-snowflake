# liquibase-snowflake
[![Build Status](https://travis-ci.com/liquibase/liquibase-snowflake.svg?branch=master)](https://travis-ci.com/liquibase/liquibase-snowflake)

[Liquibase](http://www.liquibase.org/) extension to add [Snowflake](https://www.snowflake.net/) support.  This repo should be considered the canonical version of the Snowflake extension and represents the latest and greatest version.

# Snowflake Extension
Initial release supports applying formatted sql changesets.  It might support other types of refactorings but they haven't been tested.  Some of the interesting overrides / extensions are documented below.

## Database

### currentDateTimeFunction

Snowflake's `current_timestamp` function returns a `timestamp_ltz` datetype, while
the `datetime` Liquibase datatype maps to a Snowflake `timestamp_ntz` column.  To avoid exceptions, the current_timestamp
 is cast to a `timestamp_ntz`.   Without the cast, exceptions of the form given below occur.

    SQL compilation error: Expression type does not match column data type, expecting TIMESTAMP_NTZ(9) but got TIMESTAMP_LTZ(9)

### getJdbcCatalogName

The Snowflake JDBC drivers implementation of `DatabaseMetadata.getTables()` hard codes quotes around the catalog, schema and
table names, resulting in queries of the form:

    show tables like 'DATABASECHANGELOG' in schema "sample_db"."sample_schema"

This results in the `DATABASECHANGELOG` table not being found, even after it has been created.  Since Snowflake stores
 catalog and schema names in upper case, the getJdbcCatalogName returns an upper case value.

### getJdbcSchemaName

See [getJdbcCatalogName](#getJdbcCatalogName)

## Datatype Mappings

### datetime

The `datetime` datatype in Snowflake is an alias for the datatype `timestamp_ntz`, [Date and Time Data Types](https://docs.snowflake.net/manuals/sql-reference/data-types.html#date-and-time-data-types).
The `TimestampNTZType` class clarifies this mapping from Liquibase `datetime` to Snowflake `timestamp_ntz`.


## ChangeLog

2020-02-02 : upgrade to liquibase 3.8.5


## Using the Liquibase Test Harness in Extensions
The liquibase-snowflake extension now comes with integration test support via the liquibase-test-harness. 

For more information on using the test-harness to test the snowflake extension, see [README.test-harness.md] 
