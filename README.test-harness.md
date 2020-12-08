# Using the Liquibase Test Harness in the Snowflake Extension
The liquibase-snowflake extension now comes with integration test support via the liquibase-test-harness. 
This Liquibase test framework is designed to *also* make it easy for you to test your extensions.

### Configuring your project
 
#### Adding as a dependency

If you are using maven, you can add the dependency as follows:   

```
    <dependencies>
        <dependency>
            <groupId>org.liquibase</groupId>
            <artifactId>liquibase-sdk</artifactId>
            <version>1.0-SNAPSHOT</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
```
You will need to `git clone https://github.com/liquibase/liquibase-test-harness.git` and then run `mvn install -DskipTests` to install the liquibase-sdk in your `~/.m2/repository`. 

#### Configuring your connections

We have provided a `liquibase.sdk.test.yml` file in your `src/test/resources` directory. 
This file should contain the connection information for all the databases you want the snowflake extension to be tested against.

### Executing the tests
From your IDE, right click on the `SnowflakeChangeObjectIT` test class present in `src/test/java` directory. 
Doing so, will allow you to execute all the standard change object tests in the liquibase-test-harness as well as the
snowflake specific change objects tests created exclusively to test this extension (You can find this in the 
`src/test/resources/liquibase/sdk/test/changelogs/snowflake` directory).