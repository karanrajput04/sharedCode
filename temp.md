private static final Logger HEALTH_LOG =
    LoggerFactory.getLogger("HEALTH_CHECK");

Usage:

HEALTH_LOG.info("Database connectivity successful.");
HEALTH_LOG.info("Redis is reachable.");
HEALTH_LOG.warn("Redis response time is high: {} ms", responseTime);
HEALTH_LOG.error("Database connectivity failed.", ex);

Then configure Logback:

<logger name="HEALTH_CHECK" level="INFO" additivity="false">
    <appender-ref ref="HEALTH_APPENDER"/>
</logger>
