package com.datadudes.sf2hadoop

import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.kitesdk.data.{Datasets, DatasetDescriptor, Dataset}

object DatasetUtils {

  def initializeDataset(uri: String, schema: Schema): Dataset[GenericRecord] = {
    val descriptor = new DatasetDescriptor.Builder().schema(schema).build()
    Datasets.create(uri, descriptor).asInstanceOf[Dataset[GenericRecord]]
  }

  def loadAndUpdateDataset(uri: String, schema: Schema) = {
    val origDataset = Datasets.load(uri).asInstanceOf[Dataset[GenericRecord]]
    val newDescriptor = new DatasetDescriptor.Builder(origDataset.getDescriptor).schema(schema).build()
    Datasets.update(uri, newDescriptor).asInstanceOf[Dataset[GenericRecord]]
  }

}
