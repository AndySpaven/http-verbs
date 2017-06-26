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

import uk.gov.hmrc.play.http.HttpVerbs.{DELETE => DELETE_VERB}
import uk.gov.hmrc.play.http.hooks.HttpHooks
import uk.gov.hmrc.play.http.logging.ConnectionTracing

import scala.concurrent.{ExecutionContext, Future}

trait HttpDelete extends CoreDelete with HttpTransport with HttpVerb with ConnectionTracing with HttpHooks {

  override def delete(url: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = withTracing(DELETE_VERB, url) {
    val httpResponse = doDelete(url)
    executeHooks(url, DELETE_VERB, None, httpResponse)
    mapErrors(DELETE_VERB, url, httpResponse)
  }
}
