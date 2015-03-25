package com.datadudes.sf2hadoop

import java.util.Calendar

import com.datadudes.sf2hadoop.Conversion._
import com.datadudes.sf2hadoop.DatasetUtils._
import com.datadudes.sf2hadoop.AvroUtils._
import com.typesafe.scalalogging.LazyLogging
import org.apache.avro.Schema

class SFImporter(recordSchemas: Map[String, Schema],
                 nnHostname: String,
                 nnPort: Int,
                 basePath: String,
                 sfConnection: SalesforceConnection) extends LazyLogging {

  val hdfsBasePath = if(basePath.endsWith("/")) basePath else basePath + "/"

  def initialImport(recordType: String) = {
    val schema = recordSchemas(recordType)
    val dataset = initializeDataset(datasetUri(recordType), schema)
    val writer = dataset.newWriter()
    val results = sfConnection.query(buildSFImportQuery(recordType, schema))
    var i = 0
    results.foreach { o =>
      i = i + 1
      if(i % 2000 == 0) logger.info(s"Processed $i records")
      writer.write(sfRecord2AvroRecord(o, schema))
    }
    writer.close()
  }

  def incrementalImport(recordType: String, from: Calendar, until: Calendar) = {
    val schema = recordSchemas(recordType)
    val dataset = loadAndUpdateDataset(datasetUri(recordType), schema)
    val fieldList = getFieldNames(schema).mkString(",")
    val results = sfConnection.getUpdated(recordType, fieldList, from, until)
    val writer = dataset.newWriter()
    var i = 0
    results.foreach { o =>
      i = i + 1
      if(i % 2000 == 0) logger.info(s"Processed $i records")
      writer.write(sfRecord2AvroRecord(o, schema))
    }
    writer.close()
  }

  private def buildSFImportQuery(recordType: String, schema: Schema): String = {
    val fieldList = getFieldNames(schema).mkString(",")
    s"SELECT $fieldList FROM $recordType ORDER BY CreatedDate DESC"
  }

  private def datasetUri(recordType: String) = s"dataset:hdfs://$nnHostname:$nnPort$basePath${recordType.toLowerCase}"


}
