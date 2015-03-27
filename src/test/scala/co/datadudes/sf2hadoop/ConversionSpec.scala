package co.datadudes.sf2hadoop

import com.sforce.soap.partner.sobject.SObject
import org.apache.avro.SchemaBuilder
import org.specs2.mutable.SpecificationWithJUnit

class ConversionSpec extends SpecificationWithJUnit {

  "Conversion.sfRecord2AvroRecord" should {

    "correctly find the proper type in a null-Union" in {
      val schema = SchemaBuilder
        .record("exampleType")
        .fields()
        .name("stringUnion").`type`().unionOf().stringType().and().nullType().endUnion().stringDefault(null)
        .endRecord()

      val sfRecord = new SObject("exampleType")
      sfRecord.setField("stringUnion", "somevalue")

      val avroRecord = Conversion.sfRecord2AvroRecord(sfRecord, schema)

      avroRecord.get("stringUnion") must beAnInstanceOf[String]
    }
  }
}
