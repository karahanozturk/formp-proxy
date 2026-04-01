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
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, ControllerComponents, PlayBodyParsers}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.formpproxy.nova.models.{TraderDetails, TraderInformation, TraderResponse}
import uk.gov.hmrc.formpproxy.nova.services.NovaTraderService

import scala.concurrent.{ExecutionContext, Future}

class NovaTraderControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  private trait Setup {
    implicit val ec: ExecutionContext    = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers
    private def fakeAuth: AuthAction     = new FakeAuthAction(parsers)
    val mockService: NovaTraderService   = mock[NovaTraderService]
    val controller                       = new NovaTraderController(fakeAuth, mockService, cc)
  }

  private val sampleTraderDetails = TraderDetails(
    vrn = "123456789",
    status = Some("REGD"),
    traderName = Some("ABC LTD"),
    tradingName = Some("ABC Trading"),
    addressLine1 = Some("1 Test Street"),
    addressLine2 = Some("Testville"),
    addressLine3 = None,
    addressLine4 = None,
    postcode = Some("SW1A 1AA"),
    email = Some("abc@example.com"),
    phoneNumber = Some("01234567890"),
    mobileNumber = None,
    tradeClass = Some("47"),
    tradeClassDescription = Some("Retail trade"),
    organisationType = Some("LIMITED_COMPANY"),
    effectiveRegDate = Some("2000-01-01"),
    ceasedDate = None,
    certIssuedDate = Some("2000-01-10"),
    nextReturnPeDate = Some("2026-03-31"),
    returnStagger = Some("MAR"),
    redundant = false,
    insolvent = false,
    missingTrader = false
  )

  private val sampleTraderInformation = TraderInformation(
    vrn = "123456789",
    status = Some("REGD"),
    traderName = Some("ABC LTD"),
    tradingName = Some("ABC Trading"),
    addressLine1 = Some("1 Test Street"),
    addressLine2 = None,
    addressLine3 = None,
    addressLine4 = None,
    postcode = Some("SW1A 1AA"),
    email = Some("abc@example.com"),
    bankAccountNumber = Some("12345678"),
    bankSortCode = Some("12-34-56"),
    businessType = Some("SOLE_TRADER"),
    organisationType = Some("LIMITED_COMPANY"),
    ddiAllowed = Some(true),
    tradeClass = Some("47"),
    tradeClassDescription = Some("Retail trade"),
    effectiveRegDate = Some("2000-01-01"),
    ceasedDate = None,
    certIssuedDate = Some("2000-01-10"),
    nextReturnPeDate = Some("2026-03-31"),
    returnsFrequency = Some("QUARTERLY"),
    returnStagger = Some("MAR")
  )

  "getTraderDetails" - {
    "returns 200 with trader response when userVrn only" in new Setup {
      val response = TraderResponse(Some(sampleTraderDetails), None)
      when(mockService.getAllTraderClientDetails(eqTo(123456789L), eqTo(None)))
        .thenReturn(Future.successful(response))

      val req    = FakeRequest(GET, "/nova/trader?userVrn=123456789")
      val result = controller.getTraderDetails("123456789", None)(req)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(response)
      verify(mockService).getAllTraderClientDetails(eqTo(123456789L), eqTo(None))
      verifyNoMoreInteractions(mockService)
    }

    "returns 200 with user and client trader when clientVrn provided" in new Setup {
      val response = TraderResponse(Some(sampleTraderDetails), Some(sampleTraderDetails.copy(vrn = "987654321")))
      when(mockService.getAllTraderClientDetails(eqTo(123456789L), eqTo(Some(987654321L))))
        .thenReturn(Future.successful(response))

      val req    = FakeRequest(GET, "/nova/trader?userVrn=123456789&clientVrn=987654321")
      val result = controller.getTraderDetails("123456789", Some("987654321"))(req)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(response)
    }

    "returns 400 when userVrn is not a valid number" in new Setup {
      val req    = FakeRequest(GET, "/nova/trader?userVrn=abc")
      val result = controller.getTraderDetails("abc", None)(req)

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] mustBe "Invalid userVrn"
    }

    "returns 400 when clientVrn is not a valid number" in new Setup {
      val req    = FakeRequest(GET, "/nova/trader?userVrn=123456789&clientVrn=abc")
      val result = controller.getTraderDetails("123456789", Some("abc"))(req)

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] mustBe "Invalid clientVrn"
    }

    "returns 500 when service throws" in new Setup {
      when(mockService.getAllTraderClientDetails(any[Long], any[Option[Long]]))
        .thenReturn(Future.failed(new RuntimeException("db error")))

      val req    = FakeRequest(GET, "/nova/trader?userVrn=123456789")
      val result = controller.getTraderDetails("123456789", None)(req)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "getTraderInformation" - {
    "returns 200 with trader information when found" in new Setup {
      when(mockService.getTraderInformation(eqTo(123456789L), eqTo(None)))
        .thenReturn(Future.successful(Some(sampleTraderInformation)))

      val req    = FakeRequest(GET, "/nova/trader-information?vrn=123456789")
      val result = controller.getTraderInformation("123456789", None)(req)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(sampleTraderInformation)
    }

    "returns 200 with gracePeriod passed through" in new Setup {
      when(mockService.getTraderInformation(eqTo(123456789L), eqTo(Some(7200))))
        .thenReturn(Future.successful(Some(sampleTraderInformation)))

      val req    = FakeRequest(GET, "/nova/trader-information?vrn=123456789&gracePeriod=7200")
      val result = controller.getTraderInformation("123456789", Some(7200))(req)

      status(result) mustBe OK
    }

    "returns 404 when trader not found" in new Setup {
      when(mockService.getTraderInformation(eqTo(123456789L), eqTo(None)))
        .thenReturn(Future.successful(None))

      val req    = FakeRequest(GET, "/nova/trader-information?vrn=123456789")
      val result = controller.getTraderInformation("123456789", None)(req)

      status(result) mustBe NOT_FOUND
      (contentAsJson(result) \ "code").as[String] mustBe "TRADER_NOT_FOUND"
    }

    "returns 400 when vrn is not a valid number" in new Setup {
      val req    = FakeRequest(GET, "/nova/trader-information?vrn=abc")
      val result = controller.getTraderInformation("abc", None)(req)

      status(result) mustBe BAD_REQUEST
    }

    "returns 500 when service throws" in new Setup {
      when(mockService.getTraderInformation(any[Long], any[Option[Int]]))
        .thenReturn(Future.failed(new RuntimeException("db error")))

      val req    = FakeRequest(GET, "/nova/trader-information?vrn=123456789")
      val result = controller.getTraderInformation("123456789", None)(req)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
