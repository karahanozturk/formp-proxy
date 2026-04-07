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
import uk.gov.hmrc.formpproxy.nova.models.{ClientListStatusResponse, HasClientResponse}
import uk.gov.hmrc.formpproxy.nova.services.NovaClientService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class NovaClientController @Inject() (
  authorise: AuthAction,
  service: NovaClientService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getClientList(
    credentialId: String,
    start: Option[Int],
    count: Option[Int],
    sort: Option[Int],
    ascending: Option[Boolean]
  ): Action[AnyContent] =
    authorise.async { implicit request =>
      service
        .getClientList(credentialId, start, count, sort, ascending)
        .map(response => Ok(Json.toJson(response)))
        .recover { case t: Throwable =>
          logger.error("[getClientList] failed", t)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def getClientListStatus(
    credentialId: String,
    serviceName: String,
    gracePeriod: Option[Int]
  ): Action[AnyContent] =
    authorise.async { implicit request =>
      service
        .getClientListStatus(credentialId, serviceName, gracePeriod)
        .map(status => Ok(Json.toJson(ClientListStatusResponse(status))))
        .recover { case t: Throwable =>
          logger.error("[getClientListStatus] failed", t)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def searchClients(
    credentialId: String,
    vrn: Option[String],
    name: Option[String],
    nameStart: Option[String],
    start: Option[Int],
    count: Option[Int],
    sort: Option[Int],
    ascending: Option[Boolean]
  ): Action[AnyContent] =
    authorise.async { implicit request =>
      service
        .searchClients(credentialId, vrn, name, nameStart, start, count, sort, ascending)
        .map(response => Ok(Json.toJson(response)))
        .recover { case t: Throwable =>
          logger.error("[searchClients] failed", t)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def hasClient(credentialId: String, vrn: String): Action[AnyContent] =
    authorise.async { implicit request =>
      service
        .hasClient(credentialId, vrn)
        .map(exists => Ok(Json.toJson(HasClientResponse(exists))))
        .recover { case t: Throwable =>
          logger.error("[hasClient] failed", t)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }
}
