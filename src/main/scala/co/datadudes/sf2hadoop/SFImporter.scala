package co.datadudes.sf2hadoop

import java.util.Calendar

import Conversion._
import DatasetUtils._
import AvroUtils._
import com.typesafe.scalalogging.LazyLogging
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.kitesdk.data.Dataset
import com.sforce.soap.partner.sobject.SObject

class SFImporter(recordSchemas: Map[String, Schema],
                 basePath: String,
                 sfConnection: SalesforceService) extends LazyLogging {

  val fsBasePath = if(basePath.endsWith("/")) basePath else basePath + "/"

  def initialImport(recordType: String) = {
    val schema = recordSchemas(recordType)
    val dataset = initializeDataset(datasetUri(recordType), schema)
    val results = sfConnection.query(buildSFImportQuery(recordType, schema))
    storeSFRecords(results, dataset)
  }

  def incrementalImport(recordType: String, from: Calendar, until: Calendar) = {
    val schema = recordSchemas(recordType)
    val dataset = loadAndUpdateDataset(datasetUri(recordType), schema)
    val fieldList = getFieldNames(schema).mkString(",")
    val results = sfConnection.getUpdated(recordType, fieldList, from, until)
    storeSFRecords(results, dataset)
  }

  private def storeSFRecords(records: Iterator[SObject], target: Dataset[GenericRecord]): Unit = {
    val writer = target.newWriter()
    val schema = target.getDescriptor.getSchema
    records.zipWithIndex.foreach { case (o, i) =>
      if(i % 2000 == 0) logger.info(s"Processes $i records")
      writer.write(sfRecord2AvroRecord(o, schema))
    }
    writer.close()
  }

  private def buildSFImportQuery(recordType: String, schema: Schema): String = {
    val fieldList = getFieldNames(schema).mkString(",")
    s"SELECT $fieldList FROM $recordType ORDER BY CreatedDate DESC"
  }

  private def datasetUri(recordType: String) = s"dataset:$fsBasePath${recordType.toLowerCase}"


}
