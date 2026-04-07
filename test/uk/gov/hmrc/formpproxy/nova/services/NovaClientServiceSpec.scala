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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.formpproxy.nova.models.{Client, ClientListResponse}
import uk.gov.hmrc.formpproxy.nova.repositories.NovaSource

import scala.concurrent.Future

class NovaClientServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  private trait Setup {
    val mockRepo: NovaSource       = mock[NovaSource]
    val service: NovaClientService = new NovaClientServiceImpl(mockRepo)

    val sampleResponse: ClientListResponse = ClientListResponse(
      clients = Seq(Client("TARI Limited", "123456789")),
      totalCount = 1,
      clientNameStartingCharacters = Seq("T")
    )
  }

  "getClientList" - {
    "applies defaults before delegating to repository" in new Setup {
      when(mockRepo.getAllClients(eqTo("cred-123"), eqTo(0), eqTo(-1), eqTo(0), eqTo(true)))
        .thenReturn(Future.successful(sampleResponse))

      service.getClientList("cred-123", None, None, None, None).futureValue mustBe sampleResponse
      verify(mockRepo).getAllClients(eqTo("cred-123"), eqTo(0), eqTo(-1), eqTo(0), eqTo(true))
    }
  }

  "getClientListStatus" - {
    "defaults gracePeriod to 14400" in new Setup {
      when(mockRepo.getClientListStatus(eqTo("cred-123"), eqTo("portal"), eqTo(14400)))
        .thenReturn(Future.successful(1))

      service.getClientListStatus("cred-123", "portal", None).futureValue mustBe 1
      verify(mockRepo).getClientListStatus(eqTo("cred-123"), eqTo("portal"), eqTo(14400))
    }
  }

  "searchClients" - {
    "uses vrn search when vrn is present" in new Setup {
      when(mockRepo.getClientByVrn(eqTo("cred-123"), eqTo("123456789")))
        .thenReturn(Future.successful(sampleResponse))

      service
        .searchClients("cred-123", Some("123456789"), Some("ignored"), Some("I"), None, None, None, None)
        .futureValue mustBe sampleResponse
      verify(mockRepo).getClientByVrn(eqTo("cred-123"), eqTo("123456789"))
    }

    "uses name search when vrn is blank and name is present" in new Setup {
      when(mockRepo.getClientsByName(eqTo("cred-123"), eqTo("tara"), eqTo(0), eqTo(-1), eqTo(0), eqTo(true)))
        .thenReturn(Future.successful(sampleResponse))

      service
        .searchClients("cred-123", Some(" "), Some(" tara "), Some("T"), None, None, None, None)
        .futureValue mustBe sampleResponse
      verify(mockRepo).getClientsByName(eqTo("cred-123"), eqTo("tara"), eqTo(0), eqTo(-1), eqTo(0), eqTo(true))
    }

    "uses nameStart search when vrn and name are blank" in new Setup {
      when(mockRepo.getClientsByNameStart(eqTo("cred-123"), eqTo("T"), eqTo(0), eqTo(-1), eqTo(0), eqTo(true)))
        .thenReturn(Future.successful(sampleResponse))

      service
        .searchClients("cred-123", Some(" "), Some(" "), Some(" T "), None, None, None, None)
        .futureValue mustBe sampleResponse
      verify(mockRepo).getClientsByNameStart(eqTo("cred-123"), eqTo("T"), eqTo(0), eqTo(-1), eqTo(0), eqTo(true))
    }

    "falls back to all clients when search fields are blank" in new Setup {
      when(mockRepo.getAllClients(eqTo("cred-123"), eqTo(0), eqTo(-1), eqTo(0), eqTo(true)))
        .thenReturn(Future.successful(sampleResponse))

      service
        .searchClients("cred-123", Some(" "), Some(" "), Some(" "), None, None, None, None)
        .futureValue mustBe sampleResponse
      verify(mockRepo).getAllClients(eqTo("cred-123"), eqTo(0), eqTo(-1), eqTo(0), eqTo(true))
    }

    "propagates failures from repository" in new Setup {
      when(mockRepo.getAllClients(any[String], any[Int], any[Int], any[Int], any[Boolean]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      whenReady(service.searchClients("cred-123", None, None, None, None, None, None, None).failed) { ex =>
        ex.getMessage mustBe "boom"
      }
    }
  }

  "hasClient" - {
    "delegates to repository" in new Setup {
      when(mockRepo.hasClient(eqTo("cred-123"), eqTo("123456789")))
        .thenReturn(Future.successful(true))

      service.hasClient("cred-123", "123456789").futureValue mustBe true
      verify(mockRepo).hasClient(eqTo("cred-123"), eqTo("123456789"))
    }
  }
}
