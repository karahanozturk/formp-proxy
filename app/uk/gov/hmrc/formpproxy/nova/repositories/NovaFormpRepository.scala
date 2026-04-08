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
  def getAllClients(
    credentialId: String,
    start: Int,
    count: Int,
    sort: Int,
    ascending: Boolean
  ): Future[ClientListResponse]
  def getClientByVrn(credentialId: String, vrn: String): Future[ClientListResponse]
  def getClientsByName(
    credentialId: String,
    name: String,
    start: Int,
    count: Int,
    sort: Int,
    ascending: Boolean
  ): Future[ClientListResponse]
  def getClientsByNameStart(
    credentialId: String,
    nameStart: String,
    start: Int,
    count: Int,
    sort: Int,
    ascending: Boolean
  ): Future[ClientListResponse]
  def getClientListStatus(credentialId: String, serviceName: String, gracePeriod: Int): Future[Int]
  def hasClient(credentialId: String, vrn: String): Future[Boolean]
  def getVehicleStatusDetails(vin: String): Future[Option[VehicleStatus]]
  def getVehicleCalculationData(
    fromCurrency: String,
    toCurrency: String,
    invoiceDate: java.sql.Date,
    arrivalDate: java.sql.Date
  ): Future[VehicleCalculationData]
  def getEuMemberStates(): Future[Seq[EuMemberState]]
  def retrieveNvraKnownFacts(nvraRefNumber: String): Future[NvraKnownFacts]
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

  override def getAllClients(
    credentialId: String,
    start: Int,
    count: Int,
    sort: Int,
    ascending: Boolean
  ): Future[ClientListResponse] = {
    logger.info("[NOVA] getAllClients")
    Future {
      db.withConnection { conn =>
        Using.resource(conn.prepareCall(NovaStoredProcedures.CallGetAllClients)) { cs =>
          cs.setString(1, credentialId)
          cs.setInt(2, start)
          cs.setInt(3, count)
          cs.setInt(4, sort)
          cs.setString(5, sortOrder(ascending))
          cs.registerOutParameter(6, OracleTypes.NUMBER)
          cs.registerOutParameter(7, OracleTypes.CURSOR)
          cs.registerOutParameter(8, OracleTypes.CURSOR)
          cs.execute()

          readClientListResponse(
            Some(cs.getInt(6)),
            cs.getObject(7).asInstanceOf[ResultSet],
            cs.getObject(8).asInstanceOf[ResultSet]
          )
        }
      }
    }
  }

  override def getClientByVrn(credentialId: String, vrn: String): Future[ClientListResponse] = {
    logger.info("[NOVA] getClientByVrn")
    Future {
      db.withConnection { conn =>
        Using.resource(conn.prepareCall(NovaStoredProcedures.CallGetClientByVrn)) { cs =>
          cs.setString(1, credentialId)
          cs.setString(2, vrn)
          cs.registerOutParameter(3, OracleTypes.CURSOR)
          cs.registerOutParameter(4, OracleTypes.CURSOR)
          cs.execute()

          readClientListResponse(
            None,
            cs.getObject(3).asInstanceOf[ResultSet],
            cs.getObject(4).asInstanceOf[ResultSet]
          )
        }
      }
    }
  }

  override def getClientsByName(
    credentialId: String,
    name: String,
    start: Int,
    count: Int,
    sort: Int,
    ascending: Boolean
  ): Future[ClientListResponse] = {
    logger.info("[NOVA] getClientsByName")
    Future {
      db.withConnection { conn =>
        Using.resource(conn.prepareCall(NovaStoredProcedures.CallGetClientsByName)) { cs =>
          cs.setString(1, credentialId)
          cs.setString(2, name)
          cs.setInt(3, start)
          cs.setInt(4, count)
          cs.setInt(5, sort)
          cs.setString(6, sortOrder(ascending))
          cs.registerOutParameter(7, OracleTypes.NUMBER)
          cs.registerOutParameter(8, OracleTypes.CURSOR)
          cs.registerOutParameter(9, OracleTypes.CURSOR)
          cs.execute()

          readClientListResponse(
            Some(cs.getInt(7)),
            cs.getObject(8).asInstanceOf[ResultSet],
            cs.getObject(9).asInstanceOf[ResultSet]
          )
        }
      }
    }
  }

  override def getClientsByNameStart(
    credentialId: String,
    nameStart: String,
    start: Int,
    count: Int,
    sort: Int,
    ascending: Boolean
  ): Future[ClientListResponse] = {
    logger.info("[NOVA] getClientsByNameStart")
    Future {
      db.withConnection { conn =>
        Using.resource(conn.prepareCall(NovaStoredProcedures.CallGetClientsByNameStart)) { cs =>
          cs.setString(1, credentialId)
          cs.setString(2, nameStart)
          cs.setInt(3, start)
          cs.setInt(4, count)
          cs.setInt(5, sort)
          cs.setString(6, sortOrder(ascending))
          cs.registerOutParameter(7, OracleTypes.NUMBER)
          cs.registerOutParameter(8, OracleTypes.CURSOR)
          cs.registerOutParameter(9, OracleTypes.CURSOR)
          cs.execute()

          readClientListResponse(
            Some(cs.getInt(7)),
            cs.getObject(8).asInstanceOf[ResultSet],
            cs.getObject(9).asInstanceOf[ResultSet]
          )
        }
      }
    }
  }

  override def getClientListStatus(credentialId: String, serviceName: String, gracePeriod: Int): Future[Int] = {
    logger.info("[NOVA] getClientListStatus")
    Future {
      db.withConnection { conn =>
        Using.resource(conn.prepareCall(NovaStoredProcedures.CallGetClientListStatus)) { cs =>
          cs.setString(1, credentialId)
          cs.setString(2, serviceName)
          cs.setInt(3, gracePeriod)
          cs.registerOutParameter(4, OracleTypes.NUMBER)
          cs.execute()
          cs.getInt(4)
        }
      }
    }
  }

  override def hasClient(credentialId: String, vrn: String): Future[Boolean] = {
    logger.info("[NOVA] hasClient")
    Future {
      db.withConnection { conn =>
        Using.resource(conn.prepareCall(NovaStoredProcedures.CallHasClient)) { cs =>
          cs.setString(1, credentialId)
          cs.setString(2, vrn)
          cs.registerOutParameter(3, OracleTypes.NUMBER)
          cs.execute()
          cs.getInt(3) > 0
        }
      }
    }
  }

  override def getVehicleStatusDetails(vin: String): Future[Option[VehicleStatus]] = {
    logger.info("[NOVA] getVehicleStatusDetails")
    Future {
      db.withConnection { conn =>
        Using.resource(conn.prepareCall(NovaStoredProcedures.CallGetVehicleStatusDetails)) { cs =>
          cs.setString(1, vin)
          cs.registerOutParameter(2, OracleTypes.CURSOR)
          cs.execute()

          val rs = cs.getObject(2).asInstanceOf[ResultSet]
          if (rs != null && rs.next()) Some(NovaRowMappers.readVehicleStatus(rs))
          else None
        }
      }
    }
  }

  override def getVehicleCalculationData(
    fromCurrency: String,
    toCurrency: String,
    invoiceDate: java.sql.Date,
    arrivalDate: java.sql.Date
  ): Future[VehicleCalculationData] = {
    logger.info("[NOVA] getVehicleCalculationData")
    Future {
      db.withConnection { conn =>
        Using.resource(conn.prepareCall(NovaStoredProcedures.CallGetVehicleCalculationData)) { cs =>
          cs.setString(1, fromCurrency)
          cs.setString(2, toCurrency)
          cs.setDate(3, invoiceDate)
          cs.setDate(4, arrivalDate)
          cs.registerOutParameter(5, OracleTypes.NUMBER)
          cs.registerOutParameter(6, OracleTypes.DATE)
          cs.registerOutParameter(7, OracleTypes.NUMBER)
          cs.registerOutParameter(8, OracleTypes.DATE)
          cs.registerOutParameter(9, OracleTypes.NUMBER)
          cs.registerOutParameter(10, OracleTypes.DATE)
          cs.registerOutParameter(11, OracleTypes.INTEGER)
          cs.registerOutParameter(12, OracleTypes.DATE)
          cs.registerOutParameter(13, OracleTypes.NUMBER)
          cs.registerOutParameter(14, OracleTypes.DATE)
          cs.registerOutParameter(15, OracleTypes.INTEGER)
          cs.registerOutParameter(16, OracleTypes.DATE)
          cs.registerOutParameter(17, OracleTypes.NUMBER)
          cs.execute()

          VehicleCalculationData(
            exchangeRate = Option(cs.getBigDecimal(5)).map(BigDecimal(_)),
            vatRateEffectiveDate = Option(cs.getDate(6)).map(_.toLocalDate.toString),
            vatRate = Option(cs.getBigDecimal(7)).map(BigDecimal(_)),
            minLimitEffDate = Option(cs.getDate(8)).map(_.toLocalDate.toString),
            minLimitAmount = Option(cs.getBigDecimal(9)).map(BigDecimal(_)),
            thresholdDaysEffDate = Option(cs.getDate(10)).map(_.toLocalDate.toString),
            thresholdDays = { val v = cs.getInt(11); if (cs.wasNull()) None else Some(v) },
            rateEffDate = Option(cs.getDate(12)).map(_.toLocalDate.toString),
            rateAmount = Option(cs.getBigDecimal(13)).map(BigDecimal(_)),
            maxNoOfDaysEffDate = Option(cs.getDate(14)).map(_.toLocalDate.toString),
            maxNoOfDays = { val v = cs.getInt(15); if (cs.wasNull()) None else Some(v) },
            altAmtEffDate = Option(cs.getDate(16)).map(_.toLocalDate.toString),
            altAmt = Option(cs.getBigDecimal(17)).map(BigDecimal(_))
          )
        }
      }
    }
  }

  override def getEuMemberStates(): Future[Seq[EuMemberState]] = {
    logger.info("[NOVA] getEuMemberStates")
    Future {
      db.withConnection { conn =>
        Using.resource(conn.prepareCall(NovaStoredProcedures.CallGetEuMemberStates)) { cs =>
          cs.registerOutParameter(1, OracleTypes.CURSOR)
          cs.execute()

          val rs = cs.getObject(1).asInstanceOf[ResultSet]
          Iterator
            .continually(rs)
            .takeWhile(r => r != null && r.next())
            .map(NovaRowMappers.readEuMemberState)
            .toSeq
        }
      }
    }
  }

  override def retrieveNvraKnownFacts(nvraRefNumber: String): Future[NvraKnownFacts] = {
    logger.info("[NOVA] retrieveNvraKnownFacts")
    Future {
      db.withConnection { conn =>
        Using.resource(conn.prepareCall(NovaStoredProcedures.CallRetrieveNvraKnownFacts)) { cs =>
          cs.setString(1, nvraRefNumber)
          cs.registerOutParameter(2, OracleTypes.VARCHAR)
          cs.registerOutParameter(3, OracleTypes.VARCHAR)
          cs.registerOutParameter(4, OracleTypes.VARCHAR)
          cs.registerOutParameter(5, OracleTypes.VARCHAR)
          cs.registerOutParameter(6, OracleTypes.VARCHAR)
          cs.registerOutParameter(7, OracleTypes.VARCHAR)
          cs.registerOutParameter(8, OracleTypes.VARCHAR)
          cs.registerOutParameter(9, OracleTypes.VARCHAR)
          cs.registerOutParameter(10, OracleTypes.VARCHAR)
          cs.registerOutParameter(11, OracleTypes.VARCHAR)
          cs.execute()

          NvraKnownFacts(
            nvraRefNumber = Option(cs.getString(2)),
            agentName = Option(cs.getString(3)),
            addressLine1 = Option(cs.getString(4)),
            addressLine2 = Option(cs.getString(5)),
            addressLine3 = Option(cs.getString(6)),
            addressLine4 = Option(cs.getString(7)),
            addressLine5 = Option(cs.getString(8)),
            postcode = Option(cs.getString(9)),
            abroadFlag = Option(cs.getString(10)),
            resultCode = Option(cs.getString(11)).getOrElse("001")
          )
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

  private def readClientListResponse(
    totalCount: Option[Int],
    clientListRs: ResultSet,
    clientNameCharsRs: ResultSet
  ): ClientListResponse = {
    val clients = readClients(clientListRs)
    ClientListResponse(
      clients = clients,
      totalCount = totalCount.getOrElse(clients.size),
      clientNameStartingCharacters = readClientNameStartingCharacters(clientNameCharsRs)
    )
  }

  private def readClients(rs: ResultSet): Seq[Client] =
    Iterator
      .continually(rs)
      .takeWhile(resultSet => resultSet != null && resultSet.next())
      .map(NovaRowMappers.readClient)
      .toSeq

  private def readClientNameStartingCharacters(rs: ResultSet): Seq[String] =
    Iterator
      .continually(rs)
      .takeWhile(resultSet => resultSet != null && resultSet.next())
      .flatMap(NovaRowMappers.readClientNameStartingCharacter)
      .toSeq

  private def sortOrder(ascending: Boolean): String =
    if (ascending) "ASC" else "DESC"
}
