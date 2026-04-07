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

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.formpproxy.actions.AuthAction
import uk.gov.hmrc.formpproxy.nova.services.NovaVehicleService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NovaVehicleController @Inject() (
  authorise: AuthAction,
  service: NovaVehicleService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getVehicleStatusDetails(vin: String): Action[AnyContent] =
    authorise.async { implicit request =>
      service
        .getVehicleStatusDetails(vin)
        .map {
          case Some(vehicle) => Ok(Json.toJson(vehicle))
          case None          =>
            NotFound(Json.obj("code" -> "VEHICLE_NOT_FOUND", "message" -> s"No vehicle found for VIN $vin"))
        }
        .recover { case t: Throwable =>
          logger.error("[getVehicleStatusDetails] failed", t)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def getVehicleCalculationData(
    fromCurrency: String,
    toCurrency: String,
    invoiceDate: String,
    arrivalDate: String
  ): Action[AnyContent] =
    authorise.async { implicit request =>
      (for {
        parsedInvoiceDate <- parseDate(invoiceDate, "invoiceDate")
        parsedArrivalDate <- parseDate(arrivalDate, "arrivalDate")
      } yield (parsedInvoiceDate, parsedArrivalDate)) match {
        case Left(error)                                   =>
          Future.successful(BadRequest(Json.obj("message" -> error)))
        case Right((parsedInvoiceDate, parsedArrivalDate)) =>
          service
            .getVehicleCalculationData(fromCurrency, toCurrency, parsedInvoiceDate, parsedArrivalDate)
            .map(data => Ok(Json.toJson(data)))
            .recover { case t: Throwable =>
              logger.error("[getVehicleCalculationData] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      }
    }

  private def parseDate(value: String, paramName: String): Either[String, LocalDate] =
    try Right(LocalDate.parse(value))
    catch { case _: DateTimeParseException => Left(s"Invalid $paramName: expected ISO-8601 date") }
}
