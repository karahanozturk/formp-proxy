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

import uk.gov.hmrc.formpproxy.nova.models.{Client, EuMemberState, TraderDetails, TraderInformation, VehicleStatus}
import uk.gov.hmrc.formpproxy.shared.utils.ResultSetUtils.*

import java.sql.ResultSet

object NovaRowMappers {

  def readEuMemberState(rs: ResultSet): EuMemberState =
    EuMemberState(
      countryCode = rs.getString("p_country_code"),
      countryDescription = rs.getOptionalString("p_coutry_desc"),
      euJoiningDate = Option(rs.getDate("p_eu_joining_date")).map(_.toLocalDate.toString),
      euLeavingDate = Option(rs.getDate("p_eu_leaving_date")).map(_.toLocalDate.toString),
      euAccessionaryDate = Option(rs.getDate("p_eu_accessionary_date")).map(_.toLocalDate.toString)
    )

  def readVehicleStatus(rs: ResultSet): VehicleStatus =
    VehicleStatus(
      vin = rs.getString("p_vin"),
      novaRef = rs.getOptionalString("p_nova_ref"),
      make = rs.getOptionalString("p_make"),
      model = rs.getOptionalString("p_model"),
      mileage = rs.getOptionalInt("p_mileage"),
      firstRegDate = Option(rs.getDate("p_first_reg_date")).map(_.toLocalDate.toString),
      status = rs.getOptionalString("p_status"),
      restrictionDate = Option(rs.getDate("p_restriction_date")).map(_.toLocalDate.toString),
      origin = rs.getOptionalString("p_origin")
    )

  def readClient(rs: ResultSet): Client =
    Client(
      name = rs.getOptionalString("CLIENT_NAME").map(_.trim).getOrElse(""),
      vatRegistrationNumber = rs.getOptionalString("VAT_REG_NUMBER").map(_.trim).getOrElse("")
    )

  def readClientNameStartingCharacter(rs: ResultSet): Option[String] =
    rs.getOptionalString("CLIENTNAMESTARTINGCHARACTER").map(_.trim).filter(_.nonEmpty)

  def readTraderInfo(rs: ResultSet, vrn: String): TraderDetails =
    TraderDetails(
      vrn = vrn,
      status = rs.getOptionalString("STATUS"),
      traderName = rs.getOptionalString("TRADER_NAME"),
      tradingName = rs.getOptionalString("TRADING_NAME"),
      addressLine1 = rs.getOptionalString("BUS_ADDRESS_1"),
      addressLine2 = rs.getOptionalString("BUS_ADDRESS_2"),
      addressLine3 = rs.getOptionalString("BUS_ADDRESS_3"),
      addressLine4 = rs.getOptionalString("BUS_ADDRESS_4"),
      postcode = rs.getOptionalString("BUS_POSTCODE"),
      email = rs.getOptionalString("EMAIL"),
      phoneNumber = None,
      mobileNumber = None,
      tradeClass = rs.getOptionalString("TRADE_CLASS"),
      tradeClassDescription = rs.getOptionalString("TRADE_CLASS_DESC"),
      organisationType = rs.getOptionalString("ORGANISATION_TYPE"),
      effectiveRegDate = Option(rs.getDate("EFFECTIVE_REG_DATE")).map(_.toLocalDate.toString),
      ceasedDate = Option(rs.getDate("CEASED_DATE")).map(_.toLocalDate.toString),
      certIssuedDate = Option(rs.getDate("CERT_ISSUED_DATE")).map(_.toLocalDate.toString),
      nextReturnPeDate = Option(rs.getDate("NEXT_RETURN_PE_DATE")).map(_.toLocalDate.toString),
      returnStagger = rs.getOptionalString("RETURN_STAGGER"),
      redundant = false,
      insolvent = false,
      missingTrader = false
    )

  def readAddrContact(rs: ResultSet, base: TraderDetails): TraderDetails =
    base.copy(
      phoneNumber = rs.getOptionalString("DAYTIME_PHONE"),
      mobileNumber = rs.getOptionalString("MOBILE_PHONE"),
      redundant = rs.getOptionalString("REDUNDANT_TRADER").contains("Y"),
      insolvent = rs.getOptionalInt("INSOLVENCY_STATUS").exists(c => (c >= 1 && c <= 4) || c == 8 || c == 9)
    )

  def readTraderDetails(rs: ResultSet, base: TraderDetails): TraderDetails =
    base.copy(
      missingTrader = rs.getOptionalString("MISSING_TRADER_IND").contains("Y")
    )

  def readTraderInformation(rs: ResultSet, vrn: String): TraderInformation =
    TraderInformation(
      vrn = vrn,
      status = rs.getOptionalString("STATUS"),
      traderName = rs.getOptionalString("TRADER_NAME"),
      tradingName = rs.getOptionalString("TRADING_NAME"),
      addressLine1 = rs.getOptionalString("BUS_ADDRESS_1"),
      addressLine2 = rs.getOptionalString("BUS_ADDRESS_2"),
      addressLine3 = rs.getOptionalString("BUS_ADDRESS_3"),
      addressLine4 = rs.getOptionalString("BUS_ADDRESS_4"),
      postcode = rs.getOptionalString("BUS_POSTCODE"),
      email = rs.getOptionalString("EMAIL"),
      bankAccountNumber = rs.getOptionalString("BANK_ACCT_NUMBER"),
      bankSortCode = rs.getOptionalString("BANK_SORT_CODE"),
      businessType = rs.getOptionalString("BUSINESS_TYPE"),
      organisationType = rs.getOptionalString("ORGANISATION_TYPE"),
      ddiAllowed = rs.getOptionalString("DDI_ALLOWED").map(_ == "Y"),
      tradeClass = rs.getOptionalString("TRADE_CLASS"),
      tradeClassDescription = rs.getOptionalString("TRADE_CLASS_DESC"),
      effectiveRegDate = Option(rs.getDate("EFFECTIVE_REG_DATE")).map(_.toLocalDate.toString),
      ceasedDate = Option(rs.getDate("CEASED_DATE")).map(_.toLocalDate.toString),
      certIssuedDate = Option(rs.getDate("CERT_ISSUED_DATE")).map(_.toLocalDate.toString),
      nextReturnPeDate = Option(rs.getDate("NEXT_RETURN_PE_DATE")).map(_.toLocalDate.toString),
      returnsFrequency = rs.getOptionalString("RETURNS_FREQUENCY"),
      returnStagger = rs.getOptionalString("RETURN_STAGGER")
    )
}
