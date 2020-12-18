/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.videotools.streamingapi.model;

/**
 *
 * @author xyz
 */
public class StreamerCapacity {
    
    private final String hostname;
    private final int absoluteCapacity;
    private final int nominalCapacity;
    private final int priority;
        // priority can only be 0, 1, 2, 3
        // perhaps an ENUM would be better, but how should it be stored?
        // 3: fill firs
        // 2: fill normal
        // 1: fill slower than the others
        // 0: don't fill at all

    public StreamerCapacity(String hostname,
            int hundredPercentCapacity,
            int absoluteCapacity,
            int priority) {
        
        this.hostname = hostname;
        this.absoluteCapacity = absoluteCapacity;
        this.nominalCapacity = hundredPercentCapacity;
        this.priority = priority;
    }

    public String getHostname() {
        return hostname;
    }

    public int getAbsoluteCapacity() {
        return absoluteCapacity;
    }

    public int getNominalCapacity() {
        return nominalCapacity;
    }

    public int getPriority() {
        return priority;
    }
    
}
