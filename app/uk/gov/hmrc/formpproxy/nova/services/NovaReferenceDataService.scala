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
import uk.gov.hmrc.formpproxy.nova.models.{EuMemberState, NvraKnownFacts}
import uk.gov.hmrc.formpproxy.nova.repositories.NovaSource

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@ImplementedBy(classOf[NovaReferenceDataServiceImpl])
trait NovaReferenceDataService {
  def getEuMemberStates(): Future[Seq[EuMemberState]]
  def getNvraKnownFacts(nvraRefNumber: String): Future[NvraKnownFacts]
}

@Singleton
class NovaReferenceDataServiceImpl @Inject() (repo: NovaSource) extends NovaReferenceDataService {

  override def getEuMemberStates(): Future[Seq[EuMemberState]] =
    repo.getEuMemberStates()

  override def getNvraKnownFacts(nvraRefNumber: String): Future[NvraKnownFacts] =
    repo.retrieveNvraKnownFacts(nvraRefNumber)
}
