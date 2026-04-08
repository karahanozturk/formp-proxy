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

package uk.gov.hmrc.formpproxy.nova.controllers

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, verifyNoMoreInteractions, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, PlayBodyParsers}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.formpproxy.nova.models.{EuMemberState, EuMemberStatesResponse, NvraKnownFacts}
import uk.gov.hmrc.formpproxy.nova.services.NovaReferenceDataService

import scala.concurrent.{ExecutionContext, Future}

class NovaReferenceDataControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  private trait Setup {
    implicit val ec: ExecutionContext         = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents      = stubControllerComponents()
    private val parsers: PlayBodyParsers      = cc.parsers
    private def fakeAuth: AuthAction          = new FakeAuthAction(parsers)
    val mockService: NovaReferenceDataService = mock[NovaReferenceDataService]
    val controller                            = new NovaReferenceDataController(fakeAuth, mockService, cc)
  }

  "getEuMemberStates" - {
    "returns 200 with EU member states" in new Setup {
      val states = Seq(
        EuMemberState("DE", Some("Germany"), Some("1958-01-01"), None, None),
        EuMemberState("FR", Some("France"), Some("1958-01-01"), None, None)
      )
      when(mockService.getEuMemberStates())
        .thenReturn(Future.successful(states))

      val req    = FakeRequest(GET, "/nova/eu-member-states")
      val result = controller.getEuMemberStates()(req)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(EuMemberStatesResponse(states))
      verify(mockService).getEuMemberStates()
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with empty list when no states" in new Setup {
      when(mockService.getEuMemberStates())
        .thenReturn(Future.successful(Seq.empty))

      val req    = FakeRequest(GET, "/nova/eu-member-states")
      val result = controller.getEuMemberStates()(req)

      status(result) mustBe OK
      (contentAsJson(result) \ "euMemberStates").as[Seq[EuMemberState]] mustBe empty
    }

    "returns 500 when service throws" in new Setup {
      when(mockService.getEuMemberStates())
        .thenReturn(Future.failed(new RuntimeException("db error")))

      val req    = FakeRequest(GET, "/nova/eu-member-states")
      val result = controller.getEuMemberStates()(req)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "getNvraKnownFacts" - {
    "returns 200 with known facts when data found" in new Setup {
      val facts = NvraKnownFacts(
        nvraRefNumber = Some("NVRA123456"),
        agentName = Some("Test Agent Ltd"),
        addressLine1 = Some("1 Agent Street"),
        addressLine2 = Some("Agentville"),
        addressLine3 = None,
        addressLine4 = None,
        addressLine5 = None,
        postcode = Some("AG1 1NT"),
        abroadFlag = Some("N"),
        resultCode = "000"
      )
      when(mockService.getNvraKnownFacts(eqTo("NVRA123456")))
        .thenReturn(Future.successful(facts))

      val req    = FakeRequest(GET, "/nova/nvra-known-facts?nvraRefNumber=NVRA123456")
      val result = controller.getNvraKnownFacts("NVRA123456")(req)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(facts)
      (contentAsJson(result) \ "resultCode").as[String] mustBe "000"
    }

    "returns 200 with resultCode 001 when no data found" in new Setup {
      val facts = NvraKnownFacts(
        nvraRefNumber = None,
        agentName = None,
        addressLine1 = None,
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        addressLine5 = None,
        postcode = None,
        abroadFlag = None,
        resultCode = "001"
      )
      when(mockService.getNvraKnownFacts(eqTo("NVRA999999")))
        .thenReturn(Future.successful(facts))

      val req    = FakeRequest(GET, "/nova/nvra-known-facts?nvraRefNumber=NVRA999999")
      val result = controller.getNvraKnownFacts("NVRA999999")(req)

      status(result) mustBe OK
      (contentAsJson(result) \ "resultCode").as[String] mustBe "001"
    }

    "returns 500 when service throws" in new Setup {
      when(mockService.getNvraKnownFacts(any[String]))
        .thenReturn(Future.failed(new RuntimeException("db error")))

      val req    = FakeRequest(GET, "/nova/nvra-known-facts?nvraRefNumber=NVRA123456")
      val result = controller.getNvraKnownFacts("NVRA123456")(req)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
