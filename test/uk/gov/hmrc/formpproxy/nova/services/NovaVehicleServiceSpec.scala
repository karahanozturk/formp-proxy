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
import uk.gov.hmrc.formpproxy.nova.models.{VehicleCalculationData, VehicleStatus}
import uk.gov.hmrc.formpproxy.nova.repositories.NovaSource

import java.time.LocalDate
import scala.concurrent.Future

class NovaVehicleServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  private trait Setup {
    val mockRepo: NovaSource        = mock[NovaSource]
    val service: NovaVehicleService = new NovaVehicleServiceImpl(mockRepo)
  }

  "getVehicleStatusDetails" - {
    "delegates to repository" in new Setup {
      when(mockRepo.getVehicleStatusDetails(eqTo("WBA123")))
        .thenReturn(Future.successful(None))

      service.getVehicleStatusDetails("WBA123").futureValue mustBe None
      verify(mockRepo).getVehicleStatusDetails(eqTo("WBA123"))
    }

    "propagates failure from repository" in new Setup {
      when(mockRepo.getVehicleStatusDetails(any[String]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      whenReady(service.getVehicleStatusDetails("WBA123").failed) { ex =>
        ex.getMessage mustBe "boom"
      }
    }
  }

  "getVehicleCalculationData" - {
    "delegates to repository with converted dates" in new Setup {
      val calcData = VehicleCalculationData(
        exchangeRate = Some(BigDecimal("1.15")),
        vatRateEffectiveDate = None,
        vatRate = None,
        minLimitEffDate = None,
        minLimitAmount = None,
        thresholdDaysEffDate = None,
        thresholdDays = None,
        rateEffDate = None,
        rateAmount = None,
        maxNoOfDaysEffDate = None,
        maxNoOfDays = None,
        altAmtEffDate = None,
        altAmt = None
      )

      when(
        mockRepo.getVehicleCalculationData(
          eqTo("EUR"),
          eqTo("GBP"),
          eqTo(java.sql.Date.valueOf(LocalDate.parse("2024-01-15"))),
          eqTo(java.sql.Date.valueOf(LocalDate.parse("2024-02-01")))
        )
      ).thenReturn(Future.successful(calcData))

      val result =
        service
          .getVehicleCalculationData("EUR", "GBP", LocalDate.parse("2024-01-15"), LocalDate.parse("2024-02-01"))
          .futureValue

      result.exchangeRate mustBe Some(BigDecimal("1.15"))
    }

    "propagates failure from repository" in new Setup {
      when(mockRepo.getVehicleCalculationData(any[String], any[String], any[java.sql.Date], any[java.sql.Date]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      whenReady(
        service
          .getVehicleCalculationData("EUR", "GBP", LocalDate.parse("2024-01-15"), LocalDate.parse("2024-02-01"))
          .failed
      ) { ex =>
        ex.getMessage mustBe "boom"
      }
    }
  }
}
