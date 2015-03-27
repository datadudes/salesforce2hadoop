package co.datadudes.sf2hadoop

import java.util.Calendar

import com.sforce.soap.partner.Connector
import com.sforce.soap.partner.sobject.SObject
import com.sforce.ws.ConnectorConfig
import com.sforce.soap.partner.PartnerConnection

class SalesforceService(connection: PartnerConnection)  {


  def query(q: String) = new Iterator[SObject] {

    private var queryResult = connection.query(q)
    private var batch = queryResult.getRecords.toIterator

    override def hasNext: Boolean =
      if (batch.hasNext) true
      else if (queryResult.isDone) false
      else {
        queryResult = connection.queryMore(queryResult.getQueryLocator)
        batch = queryResult.getRecords.toIterator
        true
      }
    override def next(): SObject = batch.next()
  }

  def getUpdated(recordType: String, fieldList: String, from: Calendar, until: Calendar) = new Iterator[SObject] {

    private val ids = connection.getUpdated(recordType, from, until).getIds.grouped(2000).toIterator
    private var batch = if(ids.hasNext) connection.retrieve(fieldList, recordType, ids.next()).toIterator else Iterator.empty

    override def hasNext: Boolean =
      if (batch.hasNext) true
      else if (!ids.hasNext) false
      else {
        batch = connection.retrieve(fieldList, recordType, ids.next()).toIterator
        true
      }

    override def next(): SObject = batch.next()
  }

}

object SalesforceService {
  def apply(username: String, password: String, apiVersion: String = "32.0") = {
    val config = new ConnectorConfig()
    config.setUsername(username)
    config.setPassword(password)
    config.setAuthEndpoint(s"https://login.salesforce.com/services/Soap/u/$apiVersion")

    val conn = Connector.newConnection(config)
    conn.setQueryOptions(2000)
    new SalesforceService(conn)
  }
}
