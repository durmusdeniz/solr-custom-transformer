# Usage

Simply build the code and put it in your classpath of your Solr instance (Usually a lib folder as a shared folder for extensions would be better)

After that, go to your **db-config.xml** file, add 
> transformer="solr.transformer.ExtendedRegexDealer

to your **entity** tag. 

Then for the fields that you would like to apply the transformer

> extendedRegex="regex you want comes here" 

with the other transformers you would like to use.
An example **db-config.xml** will look like below with for this config:

```xml
<?xml version="1.0" encoding="UTF-8" ?><dataConfig>
        <!-- mysql connector jar should be added to the lib or shared lib directory to use this properly -->
        <dataSource type="JdbcDataSource" driver="com.mysql.jdbc.Driver" url="jdbc:mysql://XXX/DB?options" user="user" password="pass" batchSize="-1"/>
        <document name="docs">
        <entity name="doc-entity" transformer="solr.transformer.ExtendedRegexDealer"  query="sql query comes here" >
                                <field column="field1" extendedRegex="regex" />
        </entity>
                </document>
</dataConfig>
```
Please keep in mind that you need to escape xml chars while adding your own regex above
