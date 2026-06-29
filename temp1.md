Your task is to design and develop a complete enterprise-grade application from scratch that replicates the functionality of an existing MT ↔ MX Translation Platform in java.
The application must be modular, scalable, configurable, maintainable, and suitable for banks handling millions of messages daily.
Explain complete processing pipeline.
Incoming request
↓
Validation
↓
Message detection
↓
Parser
↓
Intermediate model
↓
Rule engine
↓
Transformation
↓
Field mapping
↓
Conditional mapping
↓
Validation
↓
Serialization
↓
Output
-----------------
##Design a configuration-driven engine.##
Configuration should support
XML
JSON

#Support#
XPath
JSONPath
Text extraction
Regex
Constants
Functions
Conditions
Loops
Nested mappings
Collections
Arrays
Default values
Fallback values
Composite fields
Field concatenation
Transformations
Dynamic expressions
Nested IF
ELSE IF
ELSE

-------------------------
##Design a generic mapping engine.##
Support
MT → MX
MX → MT
XML → XML
JSON → XML
Text → XML
Text → MT
JSON → MT
XML → MT
No hardcoded field mappings.
Everything configurable.
----------------
##Support built-in functions##
substring
replace
split
join
trim
uppercase
lowercase
format
date conversion
currency conversion
amount formatting
regex
padding
lookup
concatenation
custom Java functions
Explain implementation.
------------------
##Design a rule engine supporting##
Conditions
Nested conditions
Boolean logic
Expression language
Priority
Rule chaining
Custom rules
Reusable rules
--------------------
##Design parsers for##
MT
MX XML
JSON
CSV
Text
Intermediate canonical model
--------------------------------
##VALIDATION ENGINE##
Design
Schema validation
Business validation
SWIFT validation
ISO validation
Custom validation
Configuration validation
-----------------------------
##ERROR HANDLING##
Design
Exceptions
Retry
Partial success
Logging
Audit
Recovery
-----------------------------
##LOGGING##
Design enterprise logging
Correlation IDs
Message IDs
Transaction IDs
Performance metrics
Execution time
Tracing
##TESTING##
Generate
Unit testing strategy
Integration testing
Regression testing
Performance testing
Sample test cases
##SECURITY##
Authentication
Authorization
Encryption
Secrets
Masking
Audit
##PERFORMANCE##
Thread pools
Streaming XML
Caching
Memory optimization
Parallel execution
