Salesforce2hadoop
=================

_Salesforce2hadoop_ allows you to import data from Salesforce and put it in HDFS (or your local filesystem), serialised as 
Avro. Despite its boring name, it's a powerful tool that helps you get all relevant data of your business in _one place_. 
It only needs access to the Salesforce API using a username/password combination, and the Enterprise WSDL of your 
Salesforce Organisation.

Features:

- Choose the type(s) of records you want to import
- Data types are preserved by looking at the Enterprise WSDL of your Salesforce Organisation
- Data is stored in [Avro](https://avro.apache.org/docs/current/) format, providing great compatibility with a number of tools
- Do a complete import of your data, or incrementally import only the records that have been changed since your last import
- Salesforce2hadoop keeps track for you of the last time each record type was imported.
- Stores data into any filesystem that Hadoop/KiteSDK supports. Can be HDFS but also a local filesystem.
- Built with the help of [KiteSDK](http://kitesdk.org/), Salesforce [WSC](https://github.com/forcedotcom/wsc) and our own [wsdl2avro](https://github.com/datadudes/wsdl2avro) library.
- Built for the JVM, so works on any system that has Java 7 or greater installed

## Installation

You can find compiled binaries [here on Github](https://github.com/datadudes/salesforce2hadoop/releases). Just download, 
unpack and you're good to go.

If you want to build **salesforce2hadoop** yourself, you need to have [Scala](http://www.scala-lang.org/download/install.html) 
and [SBT](http://www.scala-sbt.org/release/tutorial/Setup.html) installed. You can then build the "fat" jar as follows:

```bash
$ git clone https://github.com/datadudes/salesforce2hadoop.git
$ sbt assembly
```

The resulting jar file can be found in the `target/scala-2.11` directory.

## Usage

`sf2hadoop` is a command line application that is really simple to use. If you have Java 7 or higher installed, you 
can just use `java -jar sf2hadoop.jar` to run the application. To see what options are available, run:

```bash
$ java -jar sf2hadoop.jar --help
```

In order for _salesforce2hadoop_ to understand the structure of your Salesforce data, it has to read the Enterprise WSDL 
of your Salesforce organisation. You can find out [here](https://developer.salesforce.com/docs/atlas.en-us.api.meta/api/sforce_api_quickstart_steps_generate_wsdl.htm) 
how to generate and download it for your organisation.

`sf2hadoop` has 2 commands for importing data from Salesforce: _init_ and _update_.

#### Initial data import

Use **init** to do an initial data import from Salesforce. For each record type a dataset will be created in HDFS 
(or your local filesystem) and a full import of the desired record types will be done. This can take some time. You can 
do an inital import like this:

```bash
$ java -jar sf2hadoop.jar init -u <salesforce-username> -p <salesforce-password> -b /base/path -w /path/to/enterprise.wsdl -s /path/to/state-file recordtype1 recordtype2 ...
```

**Salesforce credentials**

As you can see, it needs your Salesforce username & password to login to the Salesforce API to fetch your data. 

**Data import directory**

It also needs a _basePath_ where it will store all the imported data. This must be in the form of a URI that Hadoop can 
understand. Currently salesforce2hadoop supports storing data in either HDFS or your local filesystem. URIs for these 
options have the following format:

- **HDFS:** `hdfs://hostname-of-namenode:port/path/to/dir` Port can be left out. Example URI when running `sf2hadoop` on 
the namenode of your Hadoop cluster: `hdfs://localhost/imports/salesforce`, this will store all data on HDFS in the 
`/imports/salesforce` directory.
- **Local filesystem**: `file:///path/to/dir` This will store all data on your local filesystem in the `/path/to/dir` directory.

Imported data will be stored under the provided base path. Data for each record type will be stored under its own 
directory, which is the record type _in lowercase_.

**Record types**

Provide `sf2hadoop` with the types of records you want to import from Salesforce. Specify the types by their Salesforce API names. Examples: `Account`, `Opportunity`, etc. Custom record types usually end with `__c`, for example: 
`Payment_Account__c`.

**State**

_Salesforce2hadoop_ will keep track of what record types have been imported at what point in time. This allows you to switch 
to incremental imports after the initial import. To make this possible, it will save the name of each record type that 
is imported, together with an import date. When doing an incremental update of the data (see below), it will read back 
the import states for each record type, and only request data from Salesforce that has been created/updated since that 
moment. After the incremental import, it will update the import date for each record type that was imported.

The _statefile_ can be stored either in HDFS or on your local filesystem. As with the basePath, you have to specify the 
path to the _statefile_ as a URI, following the guidelines above. The statefile will be created automatically, including all 
non-existing parent directories.

It is advised to store the _statefile_ on HDFS, so you can run `sf2hadoop` from any machine, without having to worry if 
the proper _statefile_ is present.

#### Incremental update

Use the **update** command to do an incremental update of record types that have previously been imported:

```bash
$ java -jar sf2hadoop.jar update -u <salesforce-username> -p <salesforce-password> -b /base/path -w /path/to/enterprise.wsdl -s /path/to/state-file recordtype1 recordtype2 ...
```

The parameters are the same as with the **init** command.

Record types that have not been previously initialized (no initial import has been done), will be skipped, and the output 
will tell you so.

Due to the limitations of the Salesforce API, `sf2hadoop` can only go back 30 days in the past when doing an incremental 
update. Trying an incremental import over a longer timespan, might result in errors or incomplete data, so it is advised 
to do an update at least monthly.

## Recipies

Here are some recipies to make the most out of _salesforce2hadoop_.

#### Import data into Hive/Impala

Once you've imported Salesforce data into HDFS using `sf2hadoop`, you can then create a _Hive_ table backed by the 
imported data (in Avro format) by running the following command in the Hive shell:

```
CREATE EXTERNAL TABLE <tablename>
    ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.avro.AvroSerDe'
    STORED AS INPUTFORMAT 'org.apache.hadoop.hive.ql.io.avro.AvroContainerInputFormat'
    OUTPUTFORMAT 'org.apache.hadoop.hive.ql.io.avro.AvroContainerOutputFormat'
    LOCATION '/base/path/recordtype_in_lowercase'
    TBLPROPERTIES ('avro.schema.url'='hdfs:///base/path/recordtype_in_lowercase/.metadata/schema.avsc');
```

This table will also be available in Impala (you might have to do a `INVALIDATE METADATA` for the table to show up). 
The reason to create it in Hive instead of in Impala directly, is that Hive can infer the table's schema from the Avro 
schema for you.

#### Update Impala table after incremental import

Each time you do an incremental import of data for which you have created a Hive/Impala table, you have to tell Impala 
that new data is available by running the following command in the Impala shell: `REFRESH <tablename>`

## Future plans

Some random TODOs:

- Allow creating Hive tables directly by using the facilities provided by KiteSDK
- Support other Hadoop-compliant filesystems, like S3

## Contributing

You're more than welcome to create issues for any bugs you find and ideas you have. Contributions in the form of pull 
requests are also very much appreciated!

## Authors

Salesforce2hadoop was created with passion by:

- [Daan Debie](https://github.com/DandyDev) - [Website](http://dandydev.net/)
- [Marcel Krcah](https://github.com/mkrcah) - [Website](http://marcelkrcah.net/)
