/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.videotools.streamingapi.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author xyz
 */
public class StreamerCapacity {
    
    private final String hostname;
    private volatile int hundredPercentCapacity;
    private volatile int absoluteCapacity;
    private volatile int priority;
        // priority can only be 0, 1, 2, 3
        // perhaps an ENUM would be better, but how should it be stored?
        // 3: fill firs
        // 2: fill normal
        // 1: fill slower than the others
        // 0: don't fill at all
    
    public enum Status {WORKING, ONHOLD, NOTREADY}
    private volatile Status status;
    
    private final AtomicInteger currentWorkload;
    
    public StreamerCapacity(String hostname,
                            int hundredPercentCapacity,
                            int absoluteCapacity,
                            int priority) {
        
        this.hostname = hostname;
        this.hundredPercentCapacity = hundredPercentCapacity;
        this.absoluteCapacity = absoluteCapacity;
        this.priority = priority;
        
        this.status = Status.NOTREADY;
        this.currentWorkload = new AtomicInteger(0);
    }

    public void update(int hundredPercentCapacity,
                       int absoluteCapacity,
                       int priority) {
        
        this.hundredPercentCapacity = hundredPercentCapacity;
        this.absoluteCapacity = absoluteCapacity;
        this.priority = priority;
    }
    
    public String getHostname() {
        return hostname;
    }

    public int getHundredPercentCapacity() {
        return hundredPercentCapacity;
    }

    public int getAbsoluteCapacity() {
        return absoluteCapacity;
    }

    public int getPriority() {
        return priority;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getCurrentWorkload() {
        return currentWorkload.get();
    }

    public void incCurrentWorkload() {
        currentWorkload.incrementAndGet();
    }
    
    public void decCurrentWorkload() {
        int newValue = currentWorkload.decrementAndGet();
        // There is a weakness here: Between these two lines a
        // getCurrentWorkload() could report this value as being negative.
        // But this is only an edge-case; the implementation with a
        // synchronized object would be a bit over-engineered.
        // Also: It should not get < 0 anyway!
        if (newValue < 0) {
            currentWorkload.set(newValue);
        }
    }
    
    @Override
    public String toString() {
        
        return String.format(
                "StreamerCapacity{hostname=%s, hundredPercentCapacity=%d, absoluteCapacity=%d, priority=%d, status=%s, currentWorkload=%d}",
                hostname, hundredPercentCapacity, absoluteCapacity, priority, status, currentWorkload.get());
    }
}
