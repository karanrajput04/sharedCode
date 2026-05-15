You are an expert enterprise Java architect and senior backend engineer.

Build a production-grade application named:

"Sanction Diagnosis System"

Purpose:
This application diagnoses why a sanction request failed by comparing sanction request data with canonical transaction data stored in MongoDB.

Tech Stack:
- Java 21
- Spring Boot 3.x
- Spring MVC
- Spring Data MongoDB
- MongoTemplate
- Maven
- Lombok
- Jackson
- Bootstrap 5
- HTML/CSS/JavaScript
- Thymeleaf

Architecture Requirements:
- Clean layered architecture
- SOLID principles
- Config-driven behavior
- Extensible design
- Enterprise coding standards
- Reusable services
- Proper exception handling
- Logging support
- DTO-based API design
- Thread-safe config loading
- No hardcoded FMI logic

---------------------------------------------------
APPLICATION FLOW
---------------------------------------------------

1. User enters:
   - UETR or TxnId
   - FMI Name
   - Region

2. Application connects to MongoDB cluster.

3. Application queries audit collection using:
   - UETR OR TxnId

4. Audit collection contains:
   - rawMsg
   - ErrorDetails
   - status
   - audit metadata

5. Extract rawMsg from audit record.

6. Detect message type dynamically from rawMsg.
   Examples:
   - pacs008
   - pacs009
   - camt056

7. Load FMI + Region + MessageType specific configuration from JSON files loaded during application startup.

8. Configuration defines:
   - source field name
   - MongoDB field path
   - comparison type
   - regex rules
   - duplicate rules
   - mandatory flags

9. Extract required values from sanction request/rawMsg.

10. Dynamically generate MongoDB queries.

11. Execute diagnosis logic:
   - matched
   - unmatched
   - duplicate
   - transformation error

12. If unmatched:
   Generate all field combinations where UETR is always mandatory.

Example:
Fields:
- UETR
- BIC
- F20

Generate:
- UETR
- UETR+BIC
- UETR+F20
- UETR+BIC+F20

13. Execute queries for all combinations.

14. Infer mismatch fields based on query results.

15. Show diagnosis report on UI.

---------------------------------------------------
PROJECT STRUCTURE
---------------------------------------------------

Generate complete project structure:

com.sanctiondiagnosis
|
|-- controller
|-- service
|-- repository
|-- model
|    |-- config
|    |-- dto
|    |-- mongo
|    |-- response
|
|-- util
|-- exception
|-- config
|-- engine
|-- parser
|-- validator

---------------------------------------------------
IMPORTANT DESIGN RULES
---------------------------------------------------

1. Use MongoTemplate ONLY.
Do NOT use static MongoRepository interfaces.

2. All query building must be dynamic.

3. All FMI behavior must come from JSON configuration.

4. No hardcoded field names.

5. Use Criteria and Query objects for Mongo queries.

6. Use enums for:
   - diagnosis status
   - comparison type
   - message type
   - error type

7. All services must use constructor injection.

8. Use Lombok annotations.

9. Add proper logging.

10. Add global exception handling.

11. Write reusable utility classes.

---------------------------------------------------
JSON CONFIGURATION DESIGN
---------------------------------------------------

Generate Java models for below JSON structure.

Example JSON:

{
  "fmi": "SAMOS",
  "region": "EU",
  "messageTypes": {
    "pacs008": {
      "collectionName": "canonical_txn",
      "duplicateAllowed": false,
      "queryFields": [
        {
          "sourceField": "UETR",
          "dbField": "MsgData.UETR",
          "comparison": "EXACT",
          "mandatory": true
        },
        {
          "sourceField": "BIC",
          "dbField": "MsgData.To.BIC",
          "comparison": "PREFIX",
          "length": 8,
          "mandatory": true
        },
        {
          "sourceField": "F20",
          "dbField": "MsgData.Field20",
          "comparison": "EXACT",
          "mandatory": false
        }
      ]
    }
  }
}

---------------------------------------------------
CONFIG LOADER REQUIREMENTS
---------------------------------------------------

Create:
- ConfigLoaderService

Requirements:
- Load all JSON config files during startup
- Read from /config folder
- Cache in memory
- Use thread-safe ConcurrentHashMap
- Key format:
  FMI_REGION_MSGTYPE

Example:
SAMOS_EU_PACS008

Add methods:
- getConfig(fmi, region, messageType)
- reloadConfigs()

---------------------------------------------------
MONGODB QUERY BUILDER
---------------------------------------------------

Create:
- QueryBuilderService

Responsibilities:
- Build dynamic MongoDB Criteria
- Handle:
  - exact match
  - regex match
  - prefix match
  - suffix match
  - case-insensitive match

Use ComparisonType enum.

Support normalization before comparison:
- trim
- uppercase
- remove spaces

Do NOT build raw Mongo query strings.

---------------------------------------------------
COMBINATION ENGINE
---------------------------------------------------

Create:
- CombinationGeneratorUtil

Requirements:
- Generate combinations of fields
- UETR must always be included
- Avoid exponential explosion
- Max 5 comparison fields

Example output:
[
 [UETR],
 [UETR, BIC],
 [UETR, F20],
 [UETR, BIC, F20]
]

---------------------------------------------------
DIAGNOSIS ENGINE
---------------------------------------------------

Create:
- DiagnosisEngineService

Responsibilities:
- Execute all query combinations
- Detect:
  - MATCHED
  - UNMATCHED
  - DUPLICATE
  - TRANSFORMATION_ERROR
  - CONFIG_ERROR
  - AUDIT_NOT_FOUND

Inference logic:
Example:

UETR -> MATCH
UETR+BIC -> MATCH
UETR+F20 -> NO_MATCH
UETR+BIC+F20 -> NO_MATCH

Inference:
F20 mismatch

Generate detailed mismatch explanation.

---------------------------------------------------
TRANSFORMATION ERROR HANDLING
---------------------------------------------------

If audit record contains:
- ErrorDetails

Then:
- classify as TRANSFORMATION_ERROR
- skip further diagnosis
- return formatted transformation error response

---------------------------------------------------
MESSAGE PARSER
---------------------------------------------------

Create:
- MessageParserService

Responsibilities:
- detect message type from rawMsg
- extract fields dynamically
- support XML parsing
- support XPath extraction
- reusable parser design

---------------------------------------------------
RESPONSE MODELS
---------------------------------------------------

Generate DTOs:

1. DiagnosisRequest
2. DiagnosisResponse
3. QueryExecutionResult
4. FieldMismatch
5. AuditDetails
6. TransformationErrorResponse

---------------------------------------------------
UI REQUIREMENTS
---------------------------------------------------

Build Bootstrap 5 dashboard page.

Sections:
1. Search Form
2. Audit Details
3. Transformation Error Panel
4. Extracted Fields Table
5. Query Execution Matrix
6. Mismatch Analysis Table
7. Final Diagnosis Summary

UI should be modern and responsive.

---------------------------------------------------
CONTROLLER REQUIREMENTS
---------------------------------------------------

Create:
- DiagnosisController

Endpoints:
GET /
POST /diagnose

Use Thymeleaf views.

---------------------------------------------------
LOGGING REQUIREMENTS
---------------------------------------------------

Add structured logs for:
- config loading
- audit lookup
- query execution
- mismatch detection
- exceptions

---------------------------------------------------
EXCEPTION HANDLING
---------------------------------------------------

Create:
- GlobalExceptionHandler

Handle:
- config missing
- mongo connection failure
- parsing failure
- invalid request
- diagnosis errors

---------------------------------------------------
AUDIT LOOKUP SERVICE
---------------------------------------------------

Create:
- AuditLookupService

Responsibilities:
- query audit collection
- search by UETR or TxnId
- return audit document
- validate audit result count

---------------------------------------------------
DUPLICATE DETECTION
---------------------------------------------------

Support:
- duplicateAllowed flag
- configurable duplicate detection
- multiple match handling

---------------------------------------------------
OUTPUT REQUIREMENTS
---------------------------------------------------

Generate:
- complete Java classes
- package structure
- enums
- DTOs
- services
- utility classes
- sample JSON config
- Thymeleaf HTML page
- Bootstrap styling
- controller code
- MongoTemplate query examples
- startup config loader
- combination algorithm
- diagnosis algorithm
- exception handling
- logging examples

---------------------------------------------------
CODING STYLE
---------------------------------------------------

- Clean enterprise code
- Small reusable methods
- Proper naming
- JavaDoc comments
- Avoid code duplication
- Prefer composition over inheritance
- Use immutable DTOs where possible

---------------------------------------------------
IMPORTANT
---------------------------------------------------

Do NOT generate simplistic demo code.

Generate scalable enterprise-grade code suitable for real production support systems handling sanction diagnosis across multiple FMI integrations.