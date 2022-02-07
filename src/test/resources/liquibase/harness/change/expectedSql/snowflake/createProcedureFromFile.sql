CREATE or replace PROCEDURE proc3()
RETURNS VARCHAR
LANGUAGE javascript
AS
$$
var rs = snowflake.execute( { sqlText:
`INSERT INTO table1 ("column 1")
SELECT 'value 1' AS "column 1" ;`
} )
return 'Done.'
$$