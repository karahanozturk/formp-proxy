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
import uk.gov.hmrc.formpproxy.nova.models.ClientListResponse
import uk.gov.hmrc.formpproxy.nova.repositories.NovaSource

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@ImplementedBy(classOf[NovaClientServiceImpl])
trait NovaClientService {
  def getClientList(
    credentialId: String,
    start: Option[Int],
    count: Option[Int],
    sort: Option[Int],
    ascending: Option[Boolean]
  ): Future[ClientListResponse]

  def getClientListStatus(credentialId: String, serviceName: String, gracePeriod: Option[Int]): Future[Int]

  def searchClients(
    credentialId: String,
    vrn: Option[String],
    name: Option[String],
    nameStart: Option[String],
    start: Option[Int],
    count: Option[Int],
    sort: Option[Int],
    ascending: Option[Boolean]
  ): Future[ClientListResponse]

  def hasClient(credentialId: String, vrn: String): Future[Boolean]
}

@Singleton
class NovaClientServiceImpl @Inject() (repo: NovaSource) extends NovaClientService {
  private val DefaultStart       = 0
  private val DefaultCount       = -1
  private val DefaultSort        = 0
  private val DefaultAscending   = true
  private val DefaultGracePeriod = 14400

  override def getClientList(
    credentialId: String,
    start: Option[Int],
    count: Option[Int],
    sort: Option[Int],
    ascending: Option[Boolean]
  ): Future[ClientListResponse] =
    repo.getAllClients(
      credentialId,
      start.getOrElse(DefaultStart),
      count.getOrElse(DefaultCount),
      sort.getOrElse(DefaultSort),
      ascending.getOrElse(DefaultAscending)
    )

  override def getClientListStatus(credentialId: String, serviceName: String, gracePeriod: Option[Int]): Future[Int] =
    repo.getClientListStatus(credentialId, serviceName, gracePeriod.getOrElse(DefaultGracePeriod))

  override def searchClients(
    credentialId: String,
    vrn: Option[String],
    name: Option[String],
    nameStart: Option[String],
    start: Option[Int],
    count: Option[Int],
    sort: Option[Int],
    ascending: Option[Boolean]
  ): Future[ClientListResponse] = {
    val normalizedVrn       = normalize(vrn)
    val normalizedName      = normalize(name)
    val normalizedNameStart = normalize(nameStart)
    val resolvedStart       = start.getOrElse(DefaultStart)
    val resolvedCount       = count.getOrElse(DefaultCount)
    val resolvedSort        = sort.getOrElse(DefaultSort)
    val resolvedAscending   = ascending.getOrElse(DefaultAscending)

    normalizedVrn
      .map(repo.getClientByVrn(credentialId, _))
      .orElse(
        normalizedName.map(
          repo.getClientsByName(credentialId, _, resolvedStart, resolvedCount, resolvedSort, resolvedAscending)
        )
      )
      .orElse(
        normalizedNameStart.map(
          repo.getClientsByNameStart(credentialId, _, resolvedStart, resolvedCount, resolvedSort, resolvedAscending)
        )
      )
      .getOrElse(repo.getAllClients(credentialId, resolvedStart, resolvedCount, resolvedSort, resolvedAscending))
  }

  override def hasClient(credentialId: String, vrn: String): Future[Boolean] =
    repo.hasClient(credentialId, vrn)

  private def normalize(value: Option[String]): Option[String] =
    value.map(_.trim).filter(_.nonEmpty)
}
