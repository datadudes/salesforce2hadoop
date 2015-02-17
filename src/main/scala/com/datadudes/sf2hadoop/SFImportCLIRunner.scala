package com.datadudes.sf2hadoop

import java.io.File
import java.net.URI
import java.util.Calendar

import com.datadudes.wsdl2avro.WSDL2Avro
import AvroUtils._
import com.typesafe.scalalogging.LazyLogging

object SFImportCLIRunner extends App with LazyLogging {

  case class Config(command: String = "", sfUsername: String = "", sfPassword: String = "",
                    nnHostname: String = "127.0.0.1", nnPort: Int = 8020, datasetBasePath: String = "",
                    sfWSDL: File = new File("."),
                    stateFile: URI = new URI("file://" + System.getProperty("user.home") + "/.sf2hadoop/state"),
                    records: Seq[String] = Seq())

  val parser = new scopt.OptionParser[Config]("sf2hadoop") {
    head("sf2hadoop", "0.1")
    cmd("init") required() action { (_, c) => c.copy(command = "init") } text "Initialize one or more new datasets and do initial full imports"
    cmd("update") required() action { (_, c) => c.copy(command = "update") } text "Update on or more datasets using incremental imports"
    note("\n")
    opt[String]('u', "username") required() action { (x, c) => c.copy(sfUsername = x)} text "Salesforce username"
    opt[String]('p', "password") required() action { (x, c) => c.copy(sfPassword = x)} text "Salesforce password"
    opt[String]('h', "host") optional() action { (x, c) => c.copy(nnHostname = x)} text "NodeName hostname/IP"
    opt[Int]('o', "port") optional() action { (x, c) => c.copy(nnPort = x)} text "NodeName port"
    opt[String]('b', "basepath") required() action { (x, c) => c.copy(datasetBasePath = x)} text "Datasets basepath"
    opt[File]('w', "wsdl") required() valueName "<file>" action { (x, c) => c.copy(sfWSDL = x)} text "Path to Salesforce Enterprise WSDL"
    opt[URI]('s', "state") optional() valueName "<URI>" action { (x, c) => c.copy(stateFile = x)} text "URI to state file to keep track of last updated timestamps"
    arg[String]("<record>...") unbounded() action { (x, c) => c.copy(records = c.records :+ x)} text "List of Salesforce record types to import"
    help("help") text "prints this usage text"
  }

  parser.parse(args, Config()) match {
    case Some(config) => handleCommand(config)

    case None => System.exit(1)
  }

  def handleCommand(config: Config) = {
    if(config.command.isEmpty) {
      println("You need to enter a valid command (init|update)")
    } else {
      val schemas = WSDL2Avro.convert(config.sfWSDL.getCanonicalPath, filterSFInternalFields)
      val connection = new SalesforceConnection(config.sfUsername, config.sfPassword)
      val importer = new SFImporter(schemas, config.nnHostname, config.nnPort, config.datasetBasePath, connection)
      val state = new ImportState(config)
      state.createDirs

      if(config.command == "init") {
        val existingStates = state.readStates
        val newStates = config.records.map { recordType =>
          val now = Calendar.getInstance()
          importer.initialImport(recordType)
          recordType -> now
        }.toMap
        val updatedStates = existingStates ++ newStates
        state.saveStates(updatedStates)
      }
      else {
        val existingStates = state.readStates
        val newStates = config.records.flatMap { recordType =>
          existingStates.get(recordType) match {
            case Some(previous) => {
              val now = Calendar.getInstance()
              importer.incrementalImport(recordType, previous, now)
              Option(recordType -> now)
            }
            case _ => {
              println(s"$recordType has never been initialized! Skipping...")
              None
            }
          }
        }.toMap
        state.saveStates(newStates)
      }
    }
  }

}
