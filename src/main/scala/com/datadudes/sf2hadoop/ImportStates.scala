package com.datadudes.sf2hadoop

import java.io.File
import java.util.Calendar
import javax.xml.bind.DatatypeConverter.{parseDateTime, printDateTime}
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets

object ImportStates {

  val DEFAULT_CONFIG_PATH = System.getProperty("user.home") + "/.sf2hadoop"
  val DEFAULT_STATE_FILE = DEFAULT_CONFIG_PATH + "/state"

  def readStates(path: String): Map[String, Calendar] = {
    try {
      scala.io.Source.fromFile(path).getLines().map { l =>
        val entries = l.split(",")
        entries(0) -> parseDateTime(entries(1))
      }.toMap
    } catch {
      case e: java.io.FileNotFoundException => Map[String, Calendar]()
    }
  }

  def saveStates(path: String, states: Map[String, Calendar]) = {
    val stateString = states.map(s => s._1 + "," + printDateTime(s._2)).mkString("\n")
    Files.write(Paths.get(path), stateString.getBytes(StandardCharsets.UTF_8))
  }

  def createConfigDir = {
    val configDir = new File(DEFAULT_CONFIG_PATH)
    if (!configDir.exists()) configDir.mkdir()
  }

}
