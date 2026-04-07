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

package uk.gov.hmrc.formpproxy.nova.repositories

object NovaStoredProcedures {
  val CallGetAllTraderClientDetails = "{ call VAT_DC_PK.getAllTraderClientDetails(?, ?, ?, ?, ?, ?, ?, ?) }"
  val CallGetTraderInformation      = "{ call VAT_DC_PK.getTraderInformation(?, ?, ?) }"
  val CallGetAllClients             = "{ call NOVA_CLIENT_SEARCH.getAllClients(?, ?, ?, ?, ?, ?, ?, ?) }"
  val CallGetClientByVrn            = "{ call NOVA_CLIENT_SEARCH.getClientByVrn(?, ?, ?, ?) }"
  val CallGetClientsByName          = "{ call NOVA_CLIENT_SEARCH.getClientsByName(?, ?, ?, ?, ?, ?, ?, ?, ?) }"
  val CallGetClientsByNameStart     = "{ call NOVA_CLIENT_SEARCH.getClientsByNameStart(?, ?, ?, ?, ?, ?, ?, ?, ?) }"
  val CallHasClient                 = "{ call NOVA_CLIENT_SEARCH.hasClient(?, ?, ?) }"
  val CallGetClientListStatus       = "{ call CLIENT_LIST_STATUS.getClientListDownloadStatus(?, ?, ?, ?) }"
}
