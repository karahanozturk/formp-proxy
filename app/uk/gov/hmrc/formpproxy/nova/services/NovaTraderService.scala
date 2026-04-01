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
import uk.gov.hmrc.formpproxy.nova.models.{TraderInformation, TraderResponse}
import uk.gov.hmrc.formpproxy.nova.repositories.NovaSource

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@ImplementedBy(classOf[NovaTraderServiceImpl])
trait NovaTraderService {
  def getAllTraderClientDetails(userVrn: Long, clientVrn: Option[Long]): Future[TraderResponse]
  def getTraderInformation(vrn: Long, gracePeriod: Option[Int]): Future[Option[TraderInformation]]
}

@Singleton
class NovaTraderServiceImpl @Inject() (repo: NovaSource) extends NovaTraderService {

  override def getAllTraderClientDetails(userVrn: Long, clientVrn: Option[Long]): Future[TraderResponse] =
    repo.getAllTraderClientDetails(userVrn, clientVrn)

  override def getTraderInformation(vrn: Long, gracePeriod: Option[Int]): Future[Option[TraderInformation]] =
    repo.getTraderInformation(vrn, gracePeriod)
}
