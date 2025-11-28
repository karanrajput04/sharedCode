package com.example.mqsim.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Message {
    private final String id;
    private final String payload;
    private final LocalDateTime timestamp;
    private boolean inProcessing;

    public Message(String payload) {
        this.id = UUID.randomUUID().toString();
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
        this.inProcessing = false;
    }

    public String getId() {
        return id;
    }

    public String getPayload() {
        return payload;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isInProcessing() {
        return inProcessing;
    }

    public void setInProcessing(boolean val) {
        this.inProcessing = val;
    }

    @Override
    public String toString() {
        return "[Message id=" + id +
                ", payload=" + payload +
                ", timestamp=" + timestamp +
                ", processing=" + inProcessing + "]";
    }
}
