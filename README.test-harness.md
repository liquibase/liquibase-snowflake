# Using the Liquibase Test Harness in the Snowflake Extension
The liquibase-snowflake extension now comes with integration test support via the liquibase-test-harness. 
This Liquibase test framework is designed to *also* make it easy for you to test your extensions.

### Configuring your project
 
#### Configuring your connections

We have provided a `harness-config.yml` file in `src/test/resources` directory. 
This file should contain the connection information for all the databases you want the snowflake extension to be tested against.

Use `harness.initScript.sql` file to create and populate test database for harness integration tests

#### Executing the tests
From your IDE, right click on the `SnowflakeChangeObjectIT` test class present in `src/test/java` directory. 
Doing so, will allow you to execute all the standard change object tests in the liquibase-test-harness as well as the
snowflake specific change objects tests created exclusively to test this extension (You can find this in the 
`src/test/resources/liquibase/harness/changelogs/snowflake` directory).