package com.datadudes.sf2hadoop

import com.sforce.soap.partner.sobject.SObject
import org.apache.avro.Schema
import org.apache.avro.Schema.Type
import org.apache.avro.generic.GenericData.Record
import org.apache.avro.generic.GenericRecordBuilder
import scala.collection.JavaConversions._

object Conversion {

  def sfRecord2AvroRecord(sfRecord: SObject, schema: Schema): Record = {
    val builder = new GenericRecordBuilder(schema)
    schema.getFields.foldLeft(builder){ (b, f) =>
      val sfField = if(sfRecord.getField(f.name) != null) convertFieldToType(sfRecord.getField(f.name()), f) else null
      b.set(f.name(), sfField)
    }.build()
  }

  private def convertFieldToType(sfFieldObject: Object, avroField: Schema.Field) = {
    val avroType = if(avroField.schema().getType == Type.UNION)
      avroField.schema().getTypes.filter(t => t.getType != Type.NULL).head.getType
    else
      avroField.schema().getType
    avroType match {
      case t if t == Type.DOUBLE => sfFieldObject.toString.toDouble
      case t if t == Type.INT => sfFieldObject.toString.toInt
      case t if t == Type.BOOLEAN => sfFieldObject.toString.toBoolean
      case t if t == Type.BYTES => sfFieldObject.asInstanceOf[Array[Byte]]
      case _ => sfFieldObject.toString
    }
  }
}
