package com.datadudes.sf2hadoop

import java.io.IOException
import java.util.Calendar
import javax.xml.bind.DatatypeConverter._

import com.datadudes.sf2hadoop.SFImportCLIRunner.Config
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

class ImportState(config: Config) {

  val stateFile = new Path(config.stateFile)

  val fs = stateFile.getFileSystem(new Configuration())

  def readStates: Map[String, Calendar] = {
    try {
      val is = fs.open(stateFile)
      scala.io.Source.fromInputStream(is).getLines().map { l =>
        val entries = l.split(",")
        entries(0) -> parseDateTime(entries(1))
      }.toMap
    } catch {
      case e: IOException => Map[String, Calendar]()
    }
  }

  def saveStates(states: Map[String, Calendar]) = {
    val stateString = states.map(s => s._1 + "," + printDateTime(s._2)).mkString("\n")
    val os = fs.create(stateFile, true)
    os.writeBytes(stateString)
    os.close()
  }

  def createDirs = {
    fs.mkdirs(stateFile.getParent)
  }

}
