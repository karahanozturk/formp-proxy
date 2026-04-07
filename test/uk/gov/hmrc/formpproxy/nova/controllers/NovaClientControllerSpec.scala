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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, PlayBodyParsers}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.formpproxy.actions.{AuthAction, FakeAuthAction}
import uk.gov.hmrc.formpproxy.nova.models.{Client, ClientListResponse}
import uk.gov.hmrc.formpproxy.nova.services.NovaClientService

import scala.concurrent.{ExecutionContext, Future}

class NovaClientControllerSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  private trait Setup {
    implicit val ec: ExecutionContext    = scala.concurrent.ExecutionContext.global
    private val cc: ControllerComponents = stubControllerComponents()
    private val parsers: PlayBodyParsers = cc.parsers
    private def fakeAuth: AuthAction     = new FakeAuthAction(parsers)
    val mockService: NovaClientService   = mock[NovaClientService]
    val controller                       = new NovaClientController(fakeAuth, mockService, cc)
  }

  private val sampleResponse = ClientListResponse(
    clients = Seq(Client("TARI Limited", "123456789")),
    totalCount = 42,
    clientNameStartingCharacters = Seq("A", "B", "J", "S")
  )

  "getClientList" - {
    "returns 200 with client list" in new Setup {
      when(mockService.getClientList(eqTo("cred-123"), eqTo(None), eqTo(None), eqTo(None), eqTo(None)))
        .thenReturn(Future.successful(sampleResponse))

      val req    = FakeRequest(GET, "/nova/client-list?credentialId=cred-123")
      val result = controller.getClientList("cred-123", None, None, None, None)(req)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(sampleResponse)
    }

    "returns 500 when service throws" in new Setup {
      when(
        mockService
          .getClientList(any[String], any[Option[Int]], any[Option[Int]], any[Option[Int]], any[Option[Boolean]])
      )
        .thenReturn(Future.failed(new RuntimeException("db error")))

      val req    = FakeRequest(GET, "/nova/client-list?credentialId=cred-123")
      val result = controller.getClientList("cred-123", None, None, None, None)(req)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "getClientListStatus" - {
    "returns 200 with status response" in new Setup {
      when(mockService.getClientListStatus(eqTo("cred-123"), eqTo("portal"), eqTo(None)))
        .thenReturn(Future.successful(1))

      val req    = FakeRequest(GET, "/nova/client-list-status?credentialId=cred-123&serviceName=portal")
      val result = controller.getClientListStatus("cred-123", "portal", None)(req)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj("status" -> 1)
    }

    "returns 500 when service throws" in new Setup {
      when(mockService.getClientListStatus(any[String], any[String], any[Option[Int]]))
        .thenReturn(Future.failed(new RuntimeException("db error")))

      val req    = FakeRequest(GET, "/nova/client-list-status?credentialId=cred-123&serviceName=portal")
      val result = controller.getClientListStatus("cred-123", "portal", None)(req)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "searchClients" - {
    "returns 200 with search results" in new Setup {
      when(
        mockService.searchClients(
          eqTo("cred-123"),
          eqTo(None),
          eqTo(Some("tara")),
          eqTo(None),
          eqTo(None),
          eqTo(None),
          eqTo(None),
          eqTo(None)
        )
      )
        .thenReturn(Future.successful(sampleResponse))

      val req    = FakeRequest(GET, "/nova/client-search?credentialId=cred-123&name=tara")
      val result = controller.searchClients("cred-123", None, Some("tara"), None, None, None, None, None)(req)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(sampleResponse)
    }

    "returns 500 when service throws" in new Setup {
      when(
        mockService.searchClients(
          any[String],
          any[Option[String]],
          any[Option[String]],
          any[Option[String]],
          any[Option[Int]],
          any[Option[Int]],
          any[Option[Int]],
          any[Option[Boolean]]
        )
      )
        .thenReturn(Future.failed(new RuntimeException("db error")))

      val req    = FakeRequest(GET, "/nova/client-search?credentialId=cred-123")
      val result = controller.searchClients("cred-123", None, None, None, None, None, None, None)(req)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "hasClient" - {
    "returns 200 with exists response" in new Setup {
      when(mockService.hasClient(eqTo("cred-123"), eqTo("123456789")))
        .thenReturn(Future.successful(true))

      val req    = FakeRequest(GET, "/nova/has-client?credentialId=cred-123&vrn=123456789")
      val result = controller.hasClient("cred-123", "123456789")(req)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj("exists" -> true)
    }

    "returns 500 when service throws" in new Setup {
      when(mockService.hasClient(any[String], any[String]))
        .thenReturn(Future.failed(new RuntimeException("db error")))

      val req    = FakeRequest(GET, "/nova/has-client?credentialId=cred-123&vrn=123456789")
      val result = controller.hasClient("cred-123", "123456789")(req)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
