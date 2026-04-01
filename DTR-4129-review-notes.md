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

Included commit(s): `6534bc3`  

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

Included commit(s): current Step 2 trader endpoints commit on this branch  
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
