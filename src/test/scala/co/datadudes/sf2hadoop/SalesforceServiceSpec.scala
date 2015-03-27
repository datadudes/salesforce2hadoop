package co.datadudes.sf2hadoop

import java.util.Calendar

import com.sforce.soap.partner.sobject.SObject
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import com.sforce.soap.partner.{QueryResult, PartnerConnection, GetUpdatedResult}

class SalesforceServiceSpec extends SpecificationWithJUnit with Mockito {

  "SalesforceService" should {

    "when using query(), keep calling queryMore until there are no more results" in new TestScope {
      val queryResult1 = new QueryResult()
      queryResult1.setDone(false)
      queryResult1.setRecords(Array(new SObject("a"), new SObject("b")))
      queryResult1.setQueryLocator("Locator1")
      val queryResult2 = new QueryResult()
      queryResult2.setDone(false)
      queryResult2.setRecords(Array(new SObject("c"), new SObject("d")))
      queryResult2.setQueryLocator("Locator2")
      val queryResult3 = new QueryResult()
      queryResult3.setDone(true)
      queryResult3.setRecords(Array(new SObject("e")))

      sfConn.query("Foo") returns queryResult1
      sfConn.queryMore("Locator1") returns queryResult2
      sfConn.queryMore("Locator2") returns queryResult3

      val sfService = new SalesforceService(sfConn)
      val results = sfService.query("Foo").toList

      there was one(sfConn).query("Foo")
      there was two(sfConn).queryMore(anyString)
      results.size must be equalTo 5
      results.map(_.getType) must contain("a", "b", "c", "d", "e")
    }

    "when using getUpdated(), batch retrieving records per 2000 ids" in new TestScope {
      val ids = (1 to 5000).map(_.toString).toArray
      val idIterator = ids.toIterator
      val updatedResult = new GetUpdatedResult()
      updatedResult.setIds(ids)

      sfConn.getUpdated(anyString, any[Calendar], any[Calendar]) returns updatedResult
      sfConn.retrieve(anyString, anyString, any[Array[String]]) returns Array(new SObject())

      val sfService = new SalesforceService(sfConn)
      val results = sfService.getUpdated("Foo", "a,b", Calendar.getInstance(), Calendar.getInstance()).toList

      there were three(sfConn).retrieve(anyString, anyString, any[Array[String]])
    }

    trait TestScope extends Scope {
      val sfConn = mock[PartnerConnection]
    }
  }
}
