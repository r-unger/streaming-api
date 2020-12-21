/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.videotools.streamingapi.actors;

import com.videotools.streamingapi.model.Serverspot;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author xyz
 */
public class LeaseStatsData {
    
    private static final int UPPER_USERLOAD = 400;
    private static final int HASH_INITIALCAP = (int)(UPPER_USERLOAD/0.6);
    /*
    HashMap vs. TreeMap:
    HashMap is faster, but unlike TreeMap, it's unordered
    HashMap uses a lot of unused memory
    a growing HashMap needs costly reordering
    if we know the final size of a HashMap, it's fast with O(1)-operations
        (it ccan be tuned with int initialCapacity, float loadFactor)
    see further: https://www.baeldung.com/java-treemap-vs-hashmap
    */
    private class Lease {
        public Serverspot serverspot;
        public String ipAddress;
        public Instant reserved;
        public Instant firstActivity;
        public Instant latestActivity;
    }

    private final Map<Integer, Lease> activeLeases;
    private final Map<Integer, Lease> staleLeases;
        // key: token
    private final Map<String, Integer> serverWorkload;
        // key: hostname
// TODO: must be a concurrent map as singleton, so that the API can access it
    private int removedLeaseCounter;
        // counts the dataPoints where the leaf was removed from the two maps
    
    public LeaseStatsData() {
        
        activeLeases = new HashMap<>(HASH_INITIALCAP);
        staleLeases = new HashMap<>(HASH_INITIALCAP);
        serverWorkload = new TreeMap<>();
        removedLeaseCounter = 0;
    }
    
    private void incWorkloadCounter(String hostname) {
        
        Integer old = serverWorkload.getOrDefault(hostname, 0);
            // if hostname wasn't there, get 0
        serverWorkload.put(hostname, old+1);
            // if hostname wasn't there, add new instead of replace
    }
    
    private void decWorkloadCounter(String hostname) {
        
        Integer old = serverWorkload.getOrDefault(hostname, 0);
            // if hostname wasn't there, get 0
        serverWorkload.put(hostname, Math.max(old-1, 0));
            // if hostname wasn't there, add new instead of replace
            // negative counts shouldn't be there,
            // but just in case, don't decrease below 0
    }
    
    public void registerServerspot(Serverspot serverspot, String ipAddress) {
        
        String hostname = serverspot.getHostname();
        Integer token = serverspot.getToken();
        
        // update serverWorkload
        incWorkloadCounter(hostname);
        
        // update activeUsers
        Lease lease = activeLeases.get(token);
        // should usually be null, except for rare edge case
        
        if (lease == null) {
            lease = new Lease();
            lease.serverspot = serverspot;
            lease.ipAddress = ipAddress;
            lease.reserved = Instant.now();
            lease.firstActivity = null;
            lease.latestActivity = null;
        } else {
            lease.serverspot = serverspot;
            lease.ipAddress = ipAddress;
            // keep the old leaf.reserved/firstActivity
            lease.latestActivity = Instant.now();
            // update the latestActivity
            // to prevent the leaf being sorted out again
        }
        
        activeLeases.put(token, lease);
    }
    
    public void addDataPoint(int token) {
        
        Lease lease = activeLeases.get(token);
        if (lease == null) {
            lease = staleLeases.get(token);
            if (lease != null) {
                // put it back to the active leases
                staleLeases.remove(token);
                activeLeases.put(token, lease);
                incWorkloadCounter(lease.serverspot.getHostname());
                    // need to inc. the counter, since it was decreased
                    // last time when the lease was put to the stale map
            } else {
                removedLeaseCounter++;
                // finally ignore them, but count the fact
            }
        }
        // now try again checking null pointer
        if (lease != null) {
            Instant now = Instant.now();
            if (lease.firstActivity == null) {
                lease.firstActivity = now;
            }
            lease.latestActivity = now;
            
            activeLeases.put(token, lease);
        }
    }
    
    public void removeStaleLeases() {
        
        Iterator <Integer> it1 = activeLeases.keySet().iterator();
        while (it1.hasNext()) {
            Integer token = it1.next();
            Lease lease = activeLeases.get(token);
            
            if (lease.latestActivity == null) {
                if (lease.reserved.isBefore(Instant.now().minusSeconds(10*60))) {
                    // this one reserved a serverspot, but never played an asset
                    decWorkloadCounter(lease.serverspot.getHostname());
                    it1.remove();
                    staleLeases.put(token, lease);
                }
            } else if (lease.latestActivity.isBefore(Instant.now().minusSeconds(5*60))) {
                decWorkloadCounter(lease.serverspot.getHostname());
                it1.remove();
                staleLeases.put(token, lease);
            }
            // TODO: better have that with two if (() || ()) - perhaps as booleans?
        }
        
        // now thin the map of staleLeases
        // remove all older than 1 day
        Iterator <Integer> it2 = staleLeases.keySet().iterator();
        while (it2.hasNext()) {
            Integer token = it2.next();
            Lease lease = staleLeases.get(token);
            
            if (lease.latestActivity == null) {
                if (lease.reserved.isBefore(Instant.now().minus(1, ChronoUnit.DAYS))) {
                    // this one reserved a serverspot, but never played an asset
                    it2.remove();
                }
            } else if (lease.latestActivity.isBefore(Instant.now().minus(1, ChronoUnit.DAYS))) {
                it2.remove();
            }
            // TODO: better have that with two if (() || ()) - perhaps as booleans?
        }
        
    }

    public Map<String, Integer> getServerWorkload() {
        return serverWorkload;
    }

    public void resetRemovedLeaseCounter() {
        removedLeaseCounter = 0;
    }

    public int getRemovedLeaseCounter() {
        return removedLeaseCounter;
    }

}
