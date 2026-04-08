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
import uk.gov.hmrc.formpproxy.nova.models.EuMemberStatesResponse
import uk.gov.hmrc.formpproxy.nova.services.NovaReferenceDataService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class NovaReferenceDataController @Inject() (
  authorise: AuthAction,
  service: NovaReferenceDataService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getEuMemberStates(): Action[AnyContent] =
    authorise.async { implicit request =>
      service
        .getEuMemberStates()
        .map(states => Ok(Json.toJson(EuMemberStatesResponse(states))))
        .recover { case t: Throwable =>
          logger.error("[getEuMemberStates] failed", t)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def getNvraKnownFacts(nvraRefNumber: String): Action[AnyContent] =
    authorise.async { implicit request =>
      service
        .getNvraKnownFacts(nvraRefNumber)
        .map(facts => Ok(Json.toJson(facts)))
        .recover { case t: Throwable =>
          logger.error("[getNvraKnownFacts] failed", t)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }
}
