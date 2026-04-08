# DTR-4129 Notes

Task: `DASS Technical Refresh DTR-4129 - [DEV] NoVA: Create formp-proxy for Nova`

This note links the implementation on this branch back to the ticket, the NoVA specs, and the AS-IS codebase.

## References

- Jira task  
  `https://jira.tools.tax.service.gov.uk/browse/DTR-4129`
- I4 - FormP-Proxy Microservice - NoVA  
  `https://confluence.tools.tax.service.gov.uk/display/RBD/I4+-+FormP-Proxy+Microservice+-+NoVA`
- I3 - RDS DataCache Proxy Microservice - NoVA  
  `https://confluence.tools.tax.service.gov.uk/display/RBD/I3+-+RDS+DataCache+Proxy+Microservice+-+NoVA`
- NoVA - Database Stored Procedures  
  `https://confluence.tools.tax.service.gov.uk/display/RBD/NoVA+-+Database+Stored+Procedures`
- AS-IS NoVA repo  
  `https://github.com/hmrc/nova`
- formp-proxy repo  
  `https://github.com/hmrc/formp-proxy`
- prh-oracle-xe repo  
  `https://github.com/hmrc/prh-oracle-xe/`

Source selection used on this branch:
- where the spec and AS-IS matched, follow that
- where they differed, check the stored procedure definitions as well and follow the AS-IS path unless there is a clear reason not to

## Step 1

Included commit(s): `DTR-4129 Add module for NoVA and update configuration`  

Summary:
- module scaffolding and configuration for NoVA support in `formp-proxy`
- one `nova` datasource
- `NovaSource` binding
- empty repository / mapper / stored procedure objects for the new module

References used:
- `DTR-4129`
- I3 - RDS DataCache Proxy Microservice - NoVA
- I4 - FormP-Proxy Microservice - NoVA
- NoVA - Database Stored Procedures
- formp-proxy repo
- prh-oracle-xe repo

Notes:
- the datasource setup for this step was based on the existing Oracle XE setup, grants, and synonyms model

## Step 2

Included commit(s): `DTR-4129 Add NoVA trader endpoints`  
Scope:
- `GET /nova/trader`
- `GET /nova/trader-information`

References used:
- `DTR-4129`
- I3 - RDS DataCache Proxy Microservice - NoVA
- NoVA - Database Stored Procedures
- AS-IS NoVA repo
- prh-oracle-xe repo

Notes:
- `/nova/trader` follows the I3 response shape with `userTrader` and optional `clientTrader`
- `/nova/trader-information` is treated as a separate, richer payload rather than a reuse of `/nova/trader`
- default `gracePeriod` is `14400` when omitted
  - source: I3 - RDS DataCache Proxy Microservice - NoVA, `Get Trader Information`
- insolvency mapping follows the AS-IS NoVA implementation
- there is a source mismatch on trader-information:
  - I3 names `NOVA_FILING_APP.getTraderInformation`
  - AS-IS NoVA uses `VAT_DC_PK.getTraderInformation`
  - the implementation follows the AS-IS path here

## Step 3

Included commit(s): `DTR-4129 Add NoVA client endpoints`  
Scope:
- `GET /nova/client-list`
- `GET /nova/client-list-status`
- `GET /nova/client-search`
- `GET /nova/has-client`

References used:
- `DTR-4129`
- I3 - RDS DataCache Proxy Microservice - NoVA
- NoVA - Database Stored Procedures
- AS-IS NoVA repo
- prh-oracle-xe repo

Notes:
- defaults for list and search follow I3: `start=0`, `count=-1`, `sort=0`, `ascending=true`
- default `gracePeriod` for `client-list-status` follows I3: `14400`
- client search routing follows the AS-IS service order:
  - `vrn`
  - `name`
  - `nameStart`
  - otherwise all clients
- `getClientByVrn` returns `totalCount` based on the number of rows returned, matching the AS-IS client search handling
- `clientNameStartingCharacters` follows the stored procedures and AS-IS behavior, using the letters available for the credential rather than narrowing them to the current filtered result set

## Step 4

Included commit(s): `DTR-4129 Add NoVA vehicle endpoints`
Scope:
- `GET /nova/vehicle-status`
- `GET /nova/vehicle-calculation-data`

References used:
- `DTR-4129`
- I3 - RDS DataCache Proxy Microservice - NoVA
- AS-IS NoVA repo
- prh-oracle-xe repo

Notes:
- `getVehicleStatusDetails` returns the raw Oracle cursor values for `status` ("secured"/"unsecured"/null) and `origin` (lowercase string), unlike the AS-IS Java which converts these to booleans (`secured`, `imported`). The proxy is a thin translation layer — nova-imports can interpret the raw values as needed.
- `getVehicleCalculationData2` has 17 parameters (4 IN + 13 OUT). OUT parameter types verified against AS-IS Java SP wrapper: `p_threshold_days` and `p_max_no_of_days` are `OracleTypes.INTEGER` (nullable), all other numbers are `OracleTypes.NUMBER`, all dates are `OracleTypes.DATE`.
- `invoiceDate` and `arrivalDate` are parsed as ISO-8601 date strings and converted to `java.sql.Date` for Oracle. Invalid dates return 400.
- VIN is not logged (PII).

## Step 5

Included commit(s): `DTR-4129 Add NoVA reference data endpoints`
Scope:
- `GET /nova/eu-member-states`
- `GET /nova/nvra-known-facts`

References used:
- `DTR-4129`
- I3 - RDS DataCache Proxy Microservice - NoVA
- AS-IS NoVA repo
- prh-oracle-xe repo

Notes:
- `getEuMemberStates` returns all rows from the cursor without filtering. The AS-IS Java filters to current EU members (joining date != null, leaving date == null) plus Croatia. That filtering is business logic for nova-imports, not the proxy.
- cursor column `p_coutry_desc` preserves the typo in the Oracle SP — the JSON response maps it to `countryDescription`
- `retrieveNVRA_KnownFacts` uses 1 IN + 10 OUT params (all VARCHAR), verified against Oracle package definition and AS-IS Java `RetrieveNvraKnownFactsSP`. Always returns 200; consumer checks `resultCode` ("000" = found, "001" = not found).
- the AS-IS Java `RetrieveNvraKnownFactsSP` does not read `out_abroad_flag` or `out_result_code` into its DTO. The proxy includes both in the response to match the I3 wiki spec.
