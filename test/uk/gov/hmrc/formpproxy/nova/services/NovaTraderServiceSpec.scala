/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.formpproxy.nova.services

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.formpproxy.nova.models.{TraderInformation, TraderResponse}
import uk.gov.hmrc.formpproxy.nova.repositories.NovaSource

import scala.concurrent.Future

class NovaTraderServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  private trait Setup {
    val mockRepo: NovaSource       = mock[NovaSource]
    val service: NovaTraderService = new NovaTraderServiceImpl(mockRepo)
  }

  "getAllTraderClientDetails" - {
    "delegates to repository" in new Setup {
      val response = TraderResponse(None, None)
      when(mockRepo.getAllTraderClientDetails(eqTo(123L), eqTo(None)))
        .thenReturn(Future.successful(response))

      service.getAllTraderClientDetails(123L, None).futureValue mustBe response
      verify(mockRepo).getAllTraderClientDetails(eqTo(123L), eqTo(None))
    }

    "propagates failure from repository" in new Setup {
      when(mockRepo.getAllTraderClientDetails(any[Long], any[Option[Long]]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      whenReady(service.getAllTraderClientDetails(123L, None).failed) { ex =>
        ex.getMessage mustBe "boom"
      }
    }
  }

  "getTraderInformation" - {
    "delegates to repository" in new Setup {
      when(mockRepo.getTraderInformation(eqTo(123L), eqTo(Some(14400))))
        .thenReturn(Future.successful(None))

      service.getTraderInformation(123L, Some(14400)).futureValue mustBe None
      verify(mockRepo).getTraderInformation(eqTo(123L), eqTo(Some(14400)))
    }

    "propagates failure from repository" in new Setup {
      when(mockRepo.getTraderInformation(any[Long], any[Option[Int]]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      whenReady(service.getTraderInformation(123L, None).failed) { ex =>
        ex.getMessage mustBe "boom"
      }
    }
  }
}
