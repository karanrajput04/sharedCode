

We observed transaction failures during the BigData JSON handoff preparation due to a mismatch in the XSD being used by our system.

Upon analysis, it appears that the updated XSD was not shared with the development team prior to deployment/processing. As a result, our implementation continued using the earlier schema version, which caused the downstream transformation and validation failures.

This has impacted transaction processing and required additional investigation and rework from our side.

Request you to:

Share the latest approved XSD immediately.
Communicate any future schema changes proactively with the development team before implementation timelines.
Ensure versioning and change notifications are formally tracked to avoid similar production issues.

Please treat this as high priority since transactions are currently impacted.
