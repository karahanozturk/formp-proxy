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
import uk.gov.hmrc.formpproxy.nova.models.{VehicleCalculationData, VehicleStatus}
import uk.gov.hmrc.formpproxy.nova.services.NovaVehicleService

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class NovaVehicleControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  private trait Setup {
    implicit val ec: ExecutionContext    = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers
    private def fakeAuth: AuthAction     = new FakeAuthAction(parsers)
    val mockService: NovaVehicleService  = mock[NovaVehicleService]
    val controller                       = new NovaVehicleController(fakeAuth, mockService, cc)
  }

  private val sampleVehicleStatus = VehicleStatus(
    vin = "WBA12345678901234",
    novaRef = Some("NOVA26E100001"),
    make = Some("BMW"),
    model = Some("3 Series"),
    mileage = Some(5000),
    firstRegDate = Some("2024-01-15"),
    status = Some("secured"),
    restrictionDate = None,
    origin = Some("import")
  )

  private val sampleCalcData = VehicleCalculationData(
    exchangeRate = Some(BigDecimal("1.1523")),
    vatRateEffectiveDate = Some("2011-01-04"),
    vatRate = Some(BigDecimal("20.0")),
    minLimitEffDate = Some("2012-04-01"),
    minLimitAmount = Some(BigDecimal("50.00")),
    thresholdDaysEffDate = Some("2012-04-01"),
    thresholdDays = Some(14),
    rateEffDate = Some("2012-04-01"),
    rateAmount = Some(BigDecimal("5.00")),
    maxNoOfDaysEffDate = Some("2012-04-01"),
    maxNoOfDays = Some(100),
    altAmtEffDate = Some("2012-04-01"),
    altAmt = Some(BigDecimal("500.00"))
  )

  "getVehicleStatusDetails" - {
    "returns 200 with vehicle status when found" in new Setup {
      when(mockService.getVehicleStatusDetails(eqTo("WBA12345678901234")))
        .thenReturn(Future.successful(Some(sampleVehicleStatus)))

      val req    = FakeRequest(GET, "/nova/vehicle-status?vin=WBA12345678901234")
      val result = controller.getVehicleStatusDetails("WBA12345678901234")(req)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(sampleVehicleStatus)
      verify(mockService).getVehicleStatusDetails(eqTo("WBA12345678901234"))
      verifyNoMoreInteractions(mockService)
    }

    "returns 404 when vehicle not found" in new Setup {
      when(mockService.getVehicleStatusDetails(eqTo("UNKNOWN")))
        .thenReturn(Future.successful(None))

      val req    = FakeRequest(GET, "/nova/vehicle-status?vin=UNKNOWN")
      val result = controller.getVehicleStatusDetails("UNKNOWN")(req)

      status(result) mustBe NOT_FOUND
      (contentAsJson(result) \ "code").as[String] mustBe "VEHICLE_NOT_FOUND"
    }

    "returns 500 when service throws" in new Setup {
      when(mockService.getVehicleStatusDetails(any[String]))
        .thenReturn(Future.failed(new RuntimeException("db error")))

      val req    = FakeRequest(GET, "/nova/vehicle-status?vin=WBA12345678901234")
      val result = controller.getVehicleStatusDetails("WBA12345678901234")(req)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "getVehicleCalculationData" - {
    "returns 200 with calculation data" in new Setup {
      when(
        mockService.getVehicleCalculationData(
          eqTo("EUR"),
          eqTo("GBP"),
          eqTo(LocalDate.parse("2024-01-15")),
          eqTo(LocalDate.parse("2024-02-01"))
        )
      ).thenReturn(Future.successful(sampleCalcData))

      val req    = FakeRequest(
        GET,
        "/nova/vehicle-calculation-data?fromCurrency=EUR&toCurrency=GBP&invoiceDate=2024-01-15&arrivalDate=2024-02-01"
      )
      val result = controller.getVehicleCalculationData("EUR", "GBP", "2024-01-15", "2024-02-01")(req)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(sampleCalcData)
    }

    "returns 400 when invoiceDate is invalid" in new Setup {
      val req    = FakeRequest(
        GET,
        "/nova/vehicle-calculation-data?fromCurrency=EUR&toCurrency=GBP&invoiceDate=bad&arrivalDate=2024-02-01"
      )
      val result = controller.getVehicleCalculationData("EUR", "GBP", "bad", "2024-02-01")(req)

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] must include("invoiceDate")
    }

    "returns 400 when arrivalDate is invalid" in new Setup {
      val req    = FakeRequest(
        GET,
        "/nova/vehicle-calculation-data?fromCurrency=EUR&toCurrency=GBP&invoiceDate=2024-01-15&arrivalDate=bad"
      )
      val result = controller.getVehicleCalculationData("EUR", "GBP", "2024-01-15", "bad")(req)

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").as[String] must include("arrivalDate")
    }

    "returns 500 when service throws" in new Setup {
      when(mockService.getVehicleCalculationData(any[String], any[String], any[LocalDate], any[LocalDate]))
        .thenReturn(Future.failed(new RuntimeException("db error")))

      val req    = FakeRequest(
        GET,
        "/nova/vehicle-calculation-data?fromCurrency=EUR&toCurrency=GBP&invoiceDate=2024-01-15&arrivalDate=2024-02-01"
      )
      val result = controller.getVehicleCalculationData("EUR", "GBP", "2024-01-15", "2024-02-01")(req)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
