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

package uk.gov.hmrc.http

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, OptionValues, WordSpecLike}
import play.api.http.HttpVerbs._
import play.api.libs.json.{Json, Writes}
import play.twirl.api.Html
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.http.test.TestHttpTransport

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class HttpPatchSpec extends WordSpecLike with Matchers with CommonHttpBehaviour with OptionValues {

  class StubbedHttpPatch(doPatchResult: Future[HttpResponse]) extends HttpPatch with ConnectionTracingCapturing with MockitoSugar with TestHttpTransport {
    val testHook1 = mock[HttpHook]
    val testHook2 = mock[HttpHook]
    val hooks = Seq(testHook1, testHook2)

    override def doPatch[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier)= doPatchResult
  }

  "HttpPatch" should {
    val testObject = TestRequestClass("a", 1)
    "be able to return plain responses" in {
      val response = new DummyHttpResponse(testBody, 200)
      val testPatch = new StubbedHttpPatch(Future.successful(response))
      testPatch.patch(url, testObject).futureValue shouldBe response
    }
    "be able to return objects deserialised from JSON" in {
      val testPatch = new StubbedHttpPatch(Future.successful(new DummyHttpResponse("""{"foo":"t","bar":10}""", 200)))
      Json.parse(testPatch.patch[TestRequestClass](url, testObject).futureValue.body).asOpt[TestClass].value should be (TestClass("t", 10))
    }

    behave like anErrorMappingHttpCall(PATCH, (url, responseF) => new StubbedHttpPatch(responseF).patch(url, testObject))
    behave like aTracingHttpCall(PATCH, "PATCH", new StubbedHttpPatch(defaultHttpResponse)) { _.patch(url, testObject) }

    "Invoke any hooks provided" in {
      import uk.gov.hmrc.http.test.Concurrent.await

      val dummyResponseFuture = Future.successful(new DummyHttpResponse(testBody, 200))
      val testPatch = new StubbedHttpPatch(dummyResponseFuture)
      await(testPatch.patch(url, testObject))

      val testJson = Json.stringify(trcreads.writes(testObject))

      verify(testPatch.testHook1)(url, "PATCH", Some(testJson), dummyResponseFuture)
      verify(testPatch.testHook2)(url, "PATCH", Some(testJson), dummyResponseFuture)
    }
  }
}
