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
import uk.gov.hmrc.formpproxy.nova.services.NovaTraderService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.Try

@Singleton
class NovaTraderController @Inject() (
  authorise: AuthAction,
  service: NovaTraderService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getTraderDetails(userVrn: String, clientVrn: Option[String]): Action[AnyContent] =
    authorise.async { implicit request =>
      (for {
        parsedUserVrn   <- Try(userVrn.toLong).toEither.left.map(_ => "Invalid userVrn")
        parsedClientVrn <- clientVrn
                             .map(v => Try(v.toLong).toEither.left.map(_ => "Invalid clientVrn").map(Some(_)))
                             .getOrElse(Right(None))
      } yield (parsedUserVrn, parsedClientVrn)) match {
        case Left(error)                             =>
          scala.concurrent.Future.successful(BadRequest(Json.obj("message" -> error)))
        case Right((parsedUserVrn, parsedClientVrn)) =>
          service
            .getAllTraderClientDetails(parsedUserVrn, parsedClientVrn)
            .map(response => Ok(Json.toJson(response)))
            .recover { case t: Throwable =>
              logger.error("[getTraderDetails] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      }
    }

  def getTraderInformation(vrn: String, gracePeriod: Option[Int]): Action[AnyContent] =
    authorise.async { implicit request =>
      Try(vrn.toLong).toOption match {
        case None            =>
          scala.concurrent.Future.successful(BadRequest(Json.obj("message" -> "Invalid vrn")))
        case Some(parsedVrn) =>
          service
            .getTraderInformation(parsedVrn, gracePeriod)
            .map {
              case Some(trader) => Ok(Json.toJson(trader))
              case None         =>
                NotFound(Json.obj("code" -> "TRADER_NOT_FOUND", "message" -> s"No trader found for VRN $vrn"))
            }
            .recover { case t: Throwable =>
              logger.error("[getTraderInformation] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
      }
    }
}
