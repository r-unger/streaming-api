package com.videotools.streamingapi.model;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.LoggerFactory;

/**
 * This class is a wrapper around the List of StreamerCapacities.
 * All the access must be synchronized, which is guaranteed
 * through this wrapper and the setters/modifiers of StreamerCapacity.
 * 
 * @author xyz
 */
public class SyncedStreamerInfo {
    
    private static final org.slf4j.Logger logger
            = LoggerFactory.getLogger(SyncedStreamerInfo.class);
    
    private final List<StreamerCapacity> capacityList;
    /*
    From the Docs: It is imperative that the user manually synchronize
    on the returned list when iterating over it:
    List list = Collections.synchronizedList(new ArrayList());
    synchronized (list) {
        Iterator i = list.iterator(); // Must be in synchronized block
        while (i.hasNext())
            foo(i.next());
    }
    */
    private final File csvFile;

    public SyncedStreamerInfo(String streamerInfoFilename) {
        
        capacityList = Collections.synchronizedList(
                new ArrayList<>());
        
        csvFile = new File(streamerInfoFilename);
        
        // do the first list fill here, just in case the responsible actor
        // forget that or shuts down in construction
        updateStreamersFromFile();
    }
    
    private void addOrUpdate(StreamerCapacity newCap) {
        
        synchronized (capacityList) {
            boolean entryExists = false;
            Iterator i = capacityList.iterator();
            while (i.hasNext()) {
                StreamerCapacity currentCap = (StreamerCapacity)i.next();
                if (currentCap.getHostname()
                        .equalsIgnoreCase(newCap.getHostname())) {
                    entryExists = true;
                    currentCap.update(
                            newCap.getHundredPercentCapacity(),
                            newCap.getAbsoluteCapacity(),
                            newCap.getPriority());
                    break;
                }
            }
            if (!entryExists) {
                capacityList.add(newCap);
            }
        }
    }
    
    public final void updateStreamersFromFile() {
        
        try {
            // read the file
            CSVParser parser = CSVParser.parse(csvFile,
                    Charset.forName("UTF-8"), CSVFormat.RFC4180);
            // Def. of RFC4180: https://tools.ietf.org/html/rfc4180
            // "... documents the format that seems to be
            // followed by most implementations"
            for (CSVRecord record : parser) {
                if (record.size() > 0) {
                    // only process lines with at least 4 entries, but skip empty lines
                    if (record.size() < 4) {
                        // TODO: avoid empty lines logging a warning; just ignore them
                        logger.warn("Problem reading {}, line {} has less than 4 entries",
                                csvFile.getPath(), parser.getCurrentLineNumber());
                    } else {
                        // skip the header:
                        // "hostname, hundredPercentCapacity, absoluteCapacity, priority"
                        if (!record.get(0).contains("hostname")) {
                            // read the rest
                            try {
                                String hostname = record.get(0).trim();
                                int hundredPercentCapacity
                                        = Integer.valueOf(record.get(1).trim());
                                int absoluteCapacity
                                        = Integer.valueOf(record.get(2).trim());
                                int priority
                                        = Integer.valueOf(record.get(3).trim());

                                StreamerCapacity cap = new StreamerCapacity(
                                        hostname, hundredPercentCapacity,
                                        absoluteCapacity, priority);
                                addOrUpdate(cap);
                            } catch (NumberFormatException ex) {
                                logger.error(
                                        "Could not convert field to number in line {}",
                                        parser.getCurrentLineNumber());
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            logger.error("IOException: Could not read file {}", csvFile.getPath());
        }
    }
    
    public Object[] getCopyOfStreamerCapList() {
        
        synchronized (capacityList) {
            return capacityList.toArray();
            /* From the Docs:
               The returned array will be "safe" in that no references to it
               are maintained by this list. (In other words, this method must
               allocate a new array even if this list is backed by an array).
               The caller is thus free to modify the returned array.
            */
        }
    }
    
    public Map<String, Integer> getCopyOfStreamerWorkloads() {
        
        Map<String, Integer> results = new TreeMap<>();
        
        synchronized (capacityList) {
            Iterator i = capacityList.iterator();
            while (i.hasNext()) {
                StreamerCapacity currentCap = (StreamerCapacity)i.next();
                results.put(
                        currentCap.getHostname(),
                        currentCap.getCurrentWorkload());
            }
        }
        
        return results;
    }
    
    public StreamerCapacity getStreamerCapacity(String hostname) {
        
        StreamerCapacity foundCap = null;
        
        synchronized (capacityList) {
            Iterator i = capacityList.iterator();
            while (i.hasNext()) {
                StreamerCapacity currentCap = (StreamerCapacity)i.next();
                if (currentCap.getHostname()
                        .equalsIgnoreCase(hostname)) {
                    foundCap = currentCap;
                    break;
                }
            }
        }
        
        return foundCap;
        // may indeed be null
    }

}
