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

import oracle.jdbc.OracleTypes
import play.api.Logging
import play.api.db.Database
import play.api.db.NamedDatabase
import uk.gov.hmrc.formpproxy.nova.models.*

import java.sql.{ResultSet, Types}
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using

trait NovaSource {
  def getAllTraderClientDetails(userVrn: Long, clientVrn: Option[Long]): Future[TraderResponse]
  def getTraderInformation(vrn: Long, gracePeriod: Option[Int]): Future[Option[TraderInformation]]
}

@Singleton
class NovaFormpRepository @Inject() (@NamedDatabase("nova") db: Database)(implicit ec: ExecutionContext)
    extends NovaSource
    with Logging {

  override def getAllTraderClientDetails(userVrn: Long, clientVrn: Option[Long]): Future[TraderResponse] = {
    logger.info("[NOVA] getAllTraderClientDetails")
    Future {
      db.withConnection { conn =>
        Using.resource(conn.prepareCall(NovaStoredProcedures.CallGetAllTraderClientDetails)) { cs =>
          cs.setLong(1, userVrn)
          clientVrn match {
            case Some(vrn) => cs.setLong(2, vrn)
            case None      => cs.setNull(2, Types.NUMERIC)
          }
          cs.registerOutParameter(3, OracleTypes.CURSOR)
          cs.registerOutParameter(4, OracleTypes.CURSOR)
          cs.registerOutParameter(5, OracleTypes.CURSOR)
          cs.registerOutParameter(6, OracleTypes.CURSOR)
          cs.registerOutParameter(7, OracleTypes.CURSOR)
          cs.registerOutParameter(8, OracleTypes.CURSOR)
          cs.execute()

          val userTrader = readTraderFromCursors(
            userVrn.toString,
            cs.getObject(3).asInstanceOf[ResultSet],
            cs.getObject(4).asInstanceOf[ResultSet],
            cs.getObject(5).asInstanceOf[ResultSet]
          )

          val clientTrader = clientVrn.flatMap { vrn =>
            readTraderFromCursors(
              vrn.toString,
              cs.getObject(6).asInstanceOf[ResultSet],
              cs.getObject(7).asInstanceOf[ResultSet],
              cs.getObject(8).asInstanceOf[ResultSet]
            )
          }

          TraderResponse(userTrader, clientTrader)
        }
      }
    }
  }

  private val DefaultGracePeriod = 14400

  override def getTraderInformation(vrn: Long, gracePeriod: Option[Int]): Future[Option[TraderInformation]] = {
    logger.info("[NOVA] getTraderInformation")
    Future {
      db.withConnection { conn =>
        Using.resource(conn.prepareCall(NovaStoredProcedures.CallGetTraderInformation)) { cs =>
          cs.setLong(1, vrn)
          cs.setInt(2, gracePeriod.getOrElse(DefaultGracePeriod))
          cs.registerOutParameter(3, OracleTypes.CURSOR)
          cs.execute()

          val rs = cs.getObject(3).asInstanceOf[ResultSet]
          if (rs != null && rs.next()) Some(NovaRowMappers.readTraderInformation(rs, vrn.toString))
          else None
        }
      }
    }
  }

  private def readTraderFromCursors(
    vrn: String,
    traderInfoRs: ResultSet,
    addrContactRs: ResultSet,
    traderDetailsRs: ResultSet
  ): Option[TraderDetails] =
    if (traderInfoRs == null || !traderInfoRs.next()) None
    else {
      val base = NovaRowMappers.readTraderInfo(traderInfoRs, vrn)

      val withContact =
        if (addrContactRs != null && addrContactRs.next()) NovaRowMappers.readAddrContact(addrContactRs, base)
        else base

      val withDetails =
        if (traderDetailsRs != null && traderDetailsRs.next())
          NovaRowMappers.readTraderDetails(traderDetailsRs, withContact)
        else withContact

      Some(withDetails)
    }
}
