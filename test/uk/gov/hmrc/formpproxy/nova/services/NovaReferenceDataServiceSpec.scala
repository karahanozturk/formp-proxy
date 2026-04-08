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
import uk.gov.hmrc.formpproxy.nova.models.{EuMemberState, NvraKnownFacts}
import uk.gov.hmrc.formpproxy.nova.repositories.NovaSource

import scala.concurrent.Future

class NovaReferenceDataServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  private trait Setup {
    val mockRepo: NovaSource              = mock[NovaSource]
    val service: NovaReferenceDataService = new NovaReferenceDataServiceImpl(mockRepo)
  }

  "getEuMemberStates" - {
    "delegates to repository" in new Setup {
      val states = Seq(EuMemberState("DE", Some("Germany"), Some("1958-01-01"), None, None))
      when(mockRepo.getEuMemberStates())
        .thenReturn(Future.successful(states))

      service.getEuMemberStates().futureValue mustBe states
      verify(mockRepo).getEuMemberStates()
    }

    "propagates failure from repository" in new Setup {
      when(mockRepo.getEuMemberStates())
        .thenReturn(Future.failed(new RuntimeException("boom")))

      whenReady(service.getEuMemberStates().failed) { ex =>
        ex.getMessage mustBe "boom"
      }
    }
  }

  "getNvraKnownFacts" - {
    "delegates to repository" in new Setup {
      val facts = NvraKnownFacts(Some("NVRA123"), Some("Agent"), None, None, None, None, None, None, None, "000")
      when(mockRepo.retrieveNvraKnownFacts(eqTo("NVRA123")))
        .thenReturn(Future.successful(facts))

      service.getNvraKnownFacts("NVRA123").futureValue mustBe facts
      verify(mockRepo).retrieveNvraKnownFacts(eqTo("NVRA123"))
    }

    "propagates failure from repository" in new Setup {
      when(mockRepo.retrieveNvraKnownFacts(any[String]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      whenReady(service.getNvraKnownFacts("NVRA123").failed) { ex =>
        ex.getMessage mustBe "boom"
      }
    }
  }
}
