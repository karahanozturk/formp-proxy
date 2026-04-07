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

import com.google.inject.ImplementedBy
import uk.gov.hmrc.formpproxy.nova.models.{VehicleCalculationData, VehicleStatus}
import uk.gov.hmrc.formpproxy.nova.repositories.NovaSource

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@ImplementedBy(classOf[NovaVehicleServiceImpl])
trait NovaVehicleService {
  def getVehicleStatusDetails(vin: String): Future[Option[VehicleStatus]]
  def getVehicleCalculationData(
    fromCurrency: String,
    toCurrency: String,
    invoiceDate: LocalDate,
    arrivalDate: LocalDate
  ): Future[VehicleCalculationData]
}

@Singleton
class NovaVehicleServiceImpl @Inject() (repo: NovaSource) extends NovaVehicleService {

  override def getVehicleStatusDetails(vin: String): Future[Option[VehicleStatus]] =
    repo.getVehicleStatusDetails(vin)

  override def getVehicleCalculationData(
    fromCurrency: String,
    toCurrency: String,
    invoiceDate: LocalDate,
    arrivalDate: LocalDate
  ): Future[VehicleCalculationData] =
    repo.getVehicleCalculationData(
      fromCurrency,
      toCurrency,
      java.sql.Date.valueOf(invoiceDate),
      java.sql.Date.valueOf(arrivalDate)
    )
}
