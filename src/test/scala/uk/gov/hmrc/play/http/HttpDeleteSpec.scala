/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.play.http

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import play.api.http.HttpVerbs._
import play.api.libs.json.Json
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.test.TestHttpTransport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HttpDeleteSpec extends WordSpecLike with Matchers with MockitoSugar with CommonHttpBehaviour with OptionValues {

  class StubbedHttpDelete(response: Future[HttpResponse]) extends HttpDelete with ConnectionTracingCapturing with TestHttpTransport {
    val testHook1 = mock[HttpHook]
    val testHook2 = mock[HttpHook]
    val hooks = Seq(testHook1, testHook2)

    def appName: String = ???
    override def doDelete(url: String)(implicit hc: HeaderCarrier) = response
  }

  "HttpDelete" should {
    "be able to return plain responses" in {
      val response = new DummyHttpResponse(testBody, 200)
      val testDelete = new StubbedHttpDelete(Future.successful(response))
      testDelete.delete(url).futureValue shouldBe response
    }

    "be able to return objects deserialised from JSON" in {
      val testDelete = new StubbedHttpDelete(Future.successful(new DummyHttpResponse("""{"foo":"t","bar":10}""", 200)))
      Json.parse(testDelete.delete(url).futureValue(PatienceConfig(Span(2, Seconds), Span(15, Millis))).body).asOpt[TestClass].value should be (TestClass("t", 10))
    }

    behave like anErrorMappingHttpCall(DELETE, (url, responseF) => new StubbedHttpDelete(responseF).delete(url))
    behave like aTracingHttpCall(DELETE, "DELETE", new StubbedHttpDelete(defaultHttpResponse)) { _.delete(url) }

    "Invoke any hooks provided" in {
      import uk.gov.hmrc.play.test.Concurrent.await

      val dummyResponseFuture = Future.successful(new DummyHttpResponse(testBody, 200))
      val testGet = new StubbedHttpDelete(dummyResponseFuture)
      await(testGet.delete(url))

      verify(testGet.testHook1)(url, "DELETE", None, dummyResponseFuture)
      verify(testGet.testHook2)(url, "DELETE", None, dummyResponseFuture)
    }
  }
}
