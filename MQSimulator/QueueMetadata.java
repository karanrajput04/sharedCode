package com.example.mqsim.model;

public class QueueMetadata {

    private final String name;
    private final String type;
    private final int maxCapacity;

    public QueueMetadata(String name, String type, int maxCapacity) {
        this.name = name;
        this.type = type;
        this.maxCapacity = maxCapacity;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    @Override
    public String toString() {
        return "QueueMetadata{name='" + name +
                "', type='" + type +
                "', maxCapacity=" + maxCapacity + '}';
    }
}
