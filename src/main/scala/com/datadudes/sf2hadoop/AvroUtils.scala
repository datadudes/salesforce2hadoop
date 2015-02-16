package com.datadudes.sf2hadoop

import com.datadudes.wsdl2avro.WSDL2Avro.BasicNode
import org.apache.avro.Schema
import scala.collection.JavaConversions._

object AvroUtils {

  def getFieldNames(schema: Schema): List[String] = {
    schema.getFields.toList.map(_.name())
  }

  def filterSFInternalFields(node: BasicNode): Boolean = node.xmlType.endsWith("QueryResult") ||
    node.xmlType.startsWith("ens:") || node.name.endsWith("__r") || node.name.endsWith("__pr") ||
    node.name.startsWith("fieldsToNull") || node.xmlType.startsWith("tns:address")

}
