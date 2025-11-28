mq-simulator/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/example/mqsim/
    │   │   ├── App.java
    │   │   ├── config/
    │   │   │   └── WebSocketConfig.java
    │   │   ├── controller/
    │   │   │   └── MQController.java
    │   │   ├── service/
    │   │   │   ├── QueueManagerService.java
    │   │   │   └── MQSimulator.java   (per-queue instance)
    │   │   ├── model/
    │   │   │   ├── Message.java
    │   │   │   ├── QueueEvent.java
    │   │   │   ├── QueueMetadata.java
    │   │   │   └── QueueStatus.java
    │   └── resources/
    │       ├── templates/dashboard.html
    │       ├── static/css/style.css
    │       └── static/js/app.js
    └── test/...
	
