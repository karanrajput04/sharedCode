# Java Message Queue (MQ) System

A comprehensive Java application that simulates a Message Queue (MQ) system with real-time IBM MQ connectivity, SSL support, and interactive command-line interface.

## 📋 Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Installation & Setup](#installation--setup)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Usage Guide](#usage-guide)
- [API Documentation](#api-documentation)
- [Error Handling](#error-handling)
- [Best Practices](#best-practices)

## ✨ Features

### Queue Management
- **Queue Depth Check**: Monitor the number of messages in the queue
- **Queue Description**: Retrieve detailed queue metadata
- **Message Status Retrieval**: View status of all messages in the queue
- **Queue Operations**: Clear queue contents or dump for inspection
- **Message Inspection**: Detailed message content inspection

### Connectivity
- **Real-time MQ Connection**: Establish secure connections to IBM MQ servers
- **SSL/TLS Support**: Encrypted communication with certificate-based authentication
- **Connection Pooling**: Efficient resource management
- **Automatic Reconnection**: Recovery from transient failures

### Configuration Management
- **Externalized Configuration**: Support for properties files and environment variables
- **Flexible SSL Setup**: Configurable truststore and keystore paths
- **Connection Parameters**: Customizable hostname, port, queue manager, and queue names
- **Security Credentials**: Encrypted credential storage options

### User Interface
- **Interactive CLI**: Command-line interface for all operations
- **User-Friendly Menu**: Easy navigation through operations
- **Real-time Feedback**: Operation status and result display
- **Input Validation**: Robust input handling and validation

## 🏗️ Architecture

```
MQ-System/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── messagequeue/
│       │           ├── config/
│       │           │   ├── MQConfiguration.java
│       │           │   └── SSLConfiguration.java
│       │           ├── model/
│       │           │   ├── MQMessage.java
│       │           │   ├── QueueInfo.java
│       │           │   └── ConnectionConfig.java
│       │           ├── service/
│       │           │   ├── MQConnectionService.java
│       │           │   ├── QueueOperationService.java
│       │           │   └── MessageInspectionService.java
│       │           ├── util/
│       │           │   ├── PropertyLoader.java
│       │           │   ├── SSLUtil.java
│       │           │   └── ValidationUtil.java
│       │           ├── exception/
│       │           │   ├── MQConnectionException.java
│       │           │   ├── QueueOperationException.java
│       │           │   └── ConfigurationException.java
│       │           └── cli/
│       │               ├── CommandLineInterface.java
│       │               ├── Command.java
│       │               └── CommandExecutor.java
│       └── resources/
│           └── mq-config.properties
└── README.md
```

## 📦 Prerequisites

### Required Software
- **Java Development Kit (JDK)**: Version 11 or higher
- **Apache Maven**: Version 3.8.0 or higher
- **IBM MQ Server**: Version 8.0 or higher (optional for testing)
- **SSL Certificates**: Truststore (.jks) and Keystore (.jks) files

### Dependencies
- IBM MQ Client Library: `com.ibm.mq:com.ibm.mq.allclient:9.4.4.0`
- Jakarta Messaging API: `jakarta.jms:jakarta.jms-api:3.1.0`
- Logging Framework: SLF4J with Logback
- JSON Processing: Jackson

## 📂 Project Structure Details

### `config/` Package
- **MQConfiguration.java**: Central configuration management
- **SSLConfiguration.java**: SSL/TLS certificate and keystore configuration

### `model/` Package
- **MQMessage.java**: Represents individual messages in queue
- **QueueInfo.java**: Queue metadata and status information
- **ConnectionConfig.java**: Connection parameters data model

### `service/` Package
- **MQConnectionService.java**: Manages connections to MQ server
- **QueueOperationService.java**: Queue operations (put, get, clear, dump)
- **MessageInspectionService.java**: Message content analysis and retrieval

### `util/` Package
- **PropertyLoader.java**: Configuration file loading and parsing
- **SSLUtil.java**: SSL context creation and certificate handling
- **ValidationUtil.java**: Input validation and parameter checking

### `exception/` Package
- Custom exception hierarchy for specific error scenarios
- Detailed error messages and recovery suggestions

### `cli/` Package
- **CommandLineInterface.java**: Interactive user menu
- **Command.java**: Command abstraction
- **CommandExecutor.java**: Command routing and execution

## 🔧 Installation & Setup

### Step 1: Clone or Create Project

```bash
mkdir MQ-System
cd MQ-System
```

### Step 2: Create Maven Project Structure

```bash
mvn archetype:generate -DgroupId=com.messagequeue \
  -DartifactId=mq-system \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false
```

### Step 3: Update pom.xml

Add the following dependencies to your `pom.xml`:

```xml
<dependencies>
    <!-- IBM MQ Client -->
    <dependency>
        <groupId>com.ibm.mq</groupId>
        <artifactId>com.ibm.mq.allclient</artifactId>
        <version>9.4.4.0</version>
    </dependency>

    <!-- Jakarta Messaging API -->
    <dependency>
        <groupId>jakarta.jms</groupId>
        <artifactId>jakarta.jms-api</artifactId>
        <version>3.1.0</version>
    </dependency>

    <!-- Logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.9</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.4.12</version>
    </dependency>

    <!-- JSON Processing -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.16.0</version>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.13.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Step 4: Set Up SSL Certificates

Place your SSL certificates in the project:

```bash
mkdir -p src/main/resources/ssl
cp truststore.jks src/main/resources/ssl/
cp keystore.jks src/main/resources/ssl/
```

### Step 5: Configure MQ Connection

Edit `src/main/resources/mq-config.properties`:

```properties
# MQ Server Configuration
mq.host=localhost
mq.port=1414
mq.queue.manager=QM1
mq.queue.name=DEV.QUEUE.1
mq.channel=DEV.APP.SVRCONN

# Authentication (if required)
mq.username=mqm
mq.password=password

# SSL Configuration
mq.ssl.enabled=true
mq.ssl.truststore.path=src/main/resources/ssl/truststore.jks
mq.ssl.truststore.password=truststorePassword
mq.ssl.keystore.path=src/main/resources/ssl/keystore.jks
mq.ssl.keystore.password=keystorePassword
mq.ssl.cipher.suite=TLS_RSA_WITH_AES_128_CBC_SHA256

# Connection Options
mq.connection.timeout=30
mq.max.retries=3
mq.retry.delay=5000
```

### Step 6: Build the Project

```bash
mvn clean package
```

## ⚙️ Configuration

### Configuration File (`mq-config.properties`)

#### MQ Server Settings
- **mq.host**: Hostname or IP address of the MQ server
- **mq.port**: Port number (default: 1414)
- **mq.queue.manager**: Name of the Queue Manager
- **mq.queue.name**: Name of the queue to connect to
- **mq.channel**: Channel name (e.g., SYSTEM.TLS.SVRCONN for SSL)

#### SSL Configuration
- **mq.ssl.enabled**: Enable/disable SSL (true/false)
- **mq.ssl.truststore.path**: Path to truststore file
- **mq.ssl.truststore.password**: Truststore password
- **mq.ssl.keystore.path**: Path to keystore file
- **mq.ssl.keystore.password**: Keystore password
- **mq.ssl.cipher.suite**: SSL cipher suite

#### Connection Options
- **mq.connection.timeout**: Connection timeout in seconds
- **mq.max.retries**: Maximum retry attempts
- **mq.retry.delay**: Delay between retries in milliseconds

### Environment Variables

Override properties using environment variables:

```bash
export MQ_HOST=your-mq-server.com
export MQ_PORT=1414
export MQ_QUEUE_MANAGER=QM1
export MQ_QUEUE_NAME=DEV.QUEUE.1
export MQ_SSL_ENABLED=true
```

## 🚀 Running the Application

### Build

```bash
mvn clean install
```

### Run the Application

```bash
java -Dcom.ibm.mq.cfg.useIBMCipherMappings=false \
  -Djavax.net.ssl.trustStore=src/main/resources/ssl/truststore.jks \
  -Djavax.net.ssl.trustStorePassword=truststorePassword \
  -Djavax.net.ssl.keyStore=src/main/resources/ssl/keystore.jks \
  -Djavax.net.ssl.keyStorePassword=keystorePassword \
  -jar target/mq-system-1.0.0.jar
```

### Run with Spring Boot (Optional)

If using Spring Boot integration:

```bash
mvn spring-boot:run
```

## 📖 Usage Guide

### Main Menu Options

When the application starts, you'll see the main menu:

```
========================================
    IBM MQ Management System v1.0
========================================
1. Connect to MQ Server
2. Check Queue Depth
3. Get Queue Description
4. Retrieve Message Statuses
5. Put Message to Queue
6. Get Message from Queue
7. Clear Queue
8. Dump Queue Contents
9. Display Connection Info
10. Disconnect
11. Exit

Select an option (1-11):
```

### Example Operations

#### 1. Connect to MQ Server
```
Select option: 1
Connecting to MQ server...
Successfully connected to QM1
Connection established: localhost:1414
```

#### 2. Check Queue Depth
```
Select option: 2
Queue depth: 42 messages
```

#### 3. Get Queue Description
```
Select option: 3
Queue Name: DEV.QUEUE.1
Queue Manager: QM1
Max Message Length: 4194304 bytes
Current Depth: 42
```

#### 4. Put Message to Queue
```
Select option: 5
Enter message text: Hello MQ System
Enter message priority (0-9) [default: 0]: 5
Message sent successfully with ID: ID:6d3f8a2c...
```

#### 5. Dump Queue Contents
```
Select option: 8
Dumping first 10 messages...
Message 1: ID=..., Priority=5, Size=256 bytes, Timestamp=...
Message 2: ID=..., Priority=0, Size=128 bytes, Timestamp=...
...
Total messages dumped: 10
```

## 📚 API Documentation

### MQConnectionService

```java
public class MQConnectionService {
    // Establish connection to MQ server
    public void connect(ConnectionConfig config) throws MQConnectionException
    
    // Disconnect from MQ server
    public void disconnect() throws MQConnectionException
    
    // Check if connected
    public boolean isConnected()
    
    // Get current connection info
    public ConnectionInfo getConnectionInfo()
}
```

### QueueOperationService

```java
public class QueueOperationService {
    // Get number of messages in queue
    public int getQueueDepth() throws QueueOperationException
    
    // Get queue metadata
    public QueueInfo getQueueDescription() throws QueueOperationException
    
    // Get all message statuses
    public List<MessageStatus> getMessageStatuses() throws QueueOperationException
    
    // Clear all messages from queue
    public int clearQueue() throws QueueOperationException
    
    // Get specified number of messages
    public List<MQMessage> dumpQueueContents(int limit) throws QueueOperationException
    
    // Put message to queue
    public String putMessage(String content, int priority) throws QueueOperationException
}
```

### MessageInspectionService

```java
public class MessageInspectionService {
    // Get message details
    public MessageDetails inspectMessage(MQMessage message)
    
    // Format message content
    public String formatMessageContent(MQMessage message)
    
    // Get message headers
    public Map<String, String> getMessageHeaders(MQMessage message)
}
```

## ⚠️ Error Handling

### Exception Hierarchy

```
Exception
├── MQConnectionException
│   ├── ConnectionTimeoutException
│   ├── AuthenticationException
│   └── SSLHandshakeException
├── QueueOperationException
│   ├── QueueNotFoundException
│   ├── MessageExceedsLimitException
│   └── QueueLockedException
└── ConfigurationException
    ├── MissingConfigurationException
    └── InvalidConfigurationException
```

### Error Recovery

The system implements automatic retry logic:

1. **Connection Failures**: Automatic reconnection with exponential backoff
2. **Queue Locks**: Retry after timeout period
3. **Message Size Violations**: Rejection with detailed error message
4. **SSL Certificate Issues**: Detailed diagnostic information

### Logging

All operations are logged to `logs/mq-system.log`:

```
2025-11-28 11:30:45.123 [main] INFO  - Application started
2025-11-28 11:30:46.456 [MQ-Connection] INFO  - Connected to localhost:1414
2025-11-28 11:30:47.789 [MQ-Operations] DEBUG - Queue depth: 42
2025-11-28 11:30:48.901 [MQ-Operations] INFO  - Message sent: ID-123456
```

## ✅ Best Practices

### 1. Connection Management
```java
// Always use try-with-resources or finally block
try {
    connectionService.connect(config);
    // Perform operations
} finally {
    connectionService.disconnect();
}
```

### 2. Message Handling
```java
// Set appropriate timeouts and retries
config.setConnectionTimeout(30);
config.setMaxRetries(3);
config.setRetryDelay(5000);
```

### 3. SSL Configuration
```java
// Always use valid certificates in production
// Disable insecure protocols
// Use strong cipher suites (TLS 1.2 or higher)
```

### 4. Resource Cleanup
```java
// Close connections properly
// Handle exceptions explicitly
// Use logging for diagnostics
```

### 5. Performance Optimization
```java
// Use connection pooling
// Batch operations when possible
// Monitor queue depth regularly
// Implement backpressure handling
```

## 🔐 Security Considerations

1. **SSL/TLS Enforcement**: Always enable SSL in production
2. **Credential Management**: Use environment variables or secure vaults for passwords
3. **Certificate Validation**: Enable certificate chain validation
4. **Audit Logging**: Enable audit logging for compliance
5. **Access Control**: Implement user authentication and authorization
6. **Network Security**: Use VPN or private networks for MQ connections

## 📊 Performance Monitoring

Monitor the following metrics:

- Queue depth trends
- Message throughput (messages/second)
- Connection latency
- Error rates
- Resource utilization (memory, CPU)

## 🐛 Troubleshooting

### Connection Issues

**Problem**: Cannot connect to MQ server
**Solution**: 
- Verify hostname and port
- Check firewall rules
- Validate SSL certificates
- Check MQ server logs

### Message Operations

**Problem**: Queue is locked
**Solution**:
- Wait for current operation to complete
- Implement exponential backoff
- Check for deadlocks in other applications

**Problem**: SSL certificate error
**Solution**:
- Regenerate certificates
- Verify truststore password
- Check certificate expiration

## 📝 License

This project is provided as-is for educational and development purposes.

## 🤝 Support

For issues or questions:
1. Check the logs directory
2. Review error messages in detail
3. Consult IBM MQ documentation
4. Implement additional logging as needed

---

**Last Updated**: November 28, 2025
**Version**: 1.0.0
