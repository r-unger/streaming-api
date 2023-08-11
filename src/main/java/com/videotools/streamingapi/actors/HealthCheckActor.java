/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.videotools.streamingapi.actors;

import akka.actor.AbstractActor;
import akka.actor.AbstractActorWithTimers;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import com.videotools.streamingapi.model.StreamerCapacity;
import com.videotools.streamingapi.model.SyncedStreamerInfo;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author xyz
 */
public class HealthCheckActor extends AbstractActorWithTimers {

    // Props to create the actor
    public static Props props() {
        return Props.create(HealthCheckActor.class, () -> new HealthCheckActor());
    }

    // Actor messages
    // Standard message to ping the actor and see some reaction in the log file
    static public class AreYouResponsive {
        public AreYouResponsive() {
        }
    }
    // Message to trigger the internal loop
    static public class HealthCheckLoop {
        public HealthCheckLoop() {
        }
    }
    // Actor messages

    private final Object TIMERKEY;
    private final Duration LOOPWAIT;
    
    private final LoggingAdapter logger;
    protected final ReceiveBuilder actorReceiveBlt;
    
    private final SyncedStreamerInfo streamerInfo;
    
    // health recordings
    private Instant sinceWhen;
    private List<Long> maxAge;
    private List<Boolean> onHold;
        // both arrays are accessed with their index at
        // streamerInfo.getCopyOfStreamerCapList()
        // maxAge/onHold are NOT part of StreamerCapacity,
        // because they are only for temporary statistics
        // Hint: when the count & order of the StreamerCapList changes,
        // then the order of these lists are also off
    
    protected HealthCheckActor() {
        
        logger = Logging.getLogger(getContext().getSystem(), this);
        
        TIMERKEY = getSelf().path().name() + " Loop";
        LOOPWAIT = Duration.of(10, ChronoUnit.SECONDS);
            // since several timeouts will let the loop duration be beyond
            // 10 seconds, the messages may have to wait in the queue
        
        streamerInfo = ActorSingleton.getInstance()
                .getSyncedStreamerInfo();
        
        // init health recordings
        resetHealthRecordings(5);
                // to start with, it's 5 streamers;
                // if more are needed, they're added
        
        actorReceiveBlt =
                receiveBuilder()
                .match(AreYouResponsive.class, areYouResponsive -> {
                    logger.debug("Got request 'Are you responsive?'");
                })
                .match(HealthCheckLoop.class, statsLoop -> {
                    logger.debug("Got request 'HealthCheckLoop'");
                    doTheLoop();
                });
    }

    private void resetHealthRecordings(int numberOfStreamers) {
        sinceWhen = Instant.MAX;
        maxAge = new ArrayList<>();
        onHold = new ArrayList<>();
        for (int i = 0; i < numberOfStreamers; i++) {
            maxAge.add(0L);
            onHold.add(false);
        }
    }
    
    private boolean liveStreamHealthy(String urlStr, int i) {
        
        boolean healthy = false;
        long lastModSince = 0;
        
        try {
            URL url = new URL(urlStr);
            // can throw a MalformedURLException
            // (i.e. the inputName possibly has a typo)
            HttpURLConnection httpCon
                    = (HttpURLConnection) url.openConnection();
            httpCon.setConnectTimeout(5000); // since it's internal, that must be enough
            httpCon.setReadTimeout(5000);    // don't think that I need it
            // can throw an IOException
            // (i.e. probably a network error)
            int httpCode = httpCon.getResponseCode();
            //System.out.print("HTTP Code: " + httpCode);
            if ((httpCode == 200) || (httpCode == 206) || (httpCode == 304)) {
                // resource is okay, so proceed
                long lastModified = httpCon.getHeaderFieldDate("Last-Modified", 0);
                lastModSince = Instant.now().toEpochMilli() - lastModified;
                if (lastModSince < 0) {
                    logger.error("lastModified is in the future");
                } else if (lastModSince > 24*60*60*1000) {
                    logger.error("lastModified is in more than a day old");
                }
                
                if (lastModSince < 15000) { // < 15 seconds
                    healthy = true;
                } else {
                    logger.warning("Resource {} is {} old",
                            url.toExternalForm(), Duration.ofMillis(lastModSince).toString());
                }
            } else {
                // some server error, possibly 404 (not found)
                // report a StreamerCapacity.Status.NOTREADY
                logger.debug("URL '" + url.toExternalForm()
                        + "' connection with HTTP error code " + httpCode);
            }
        } catch (MalformedURLException ex) {
            logger.error("URL {} is not correct; {}",
                    urlStr, ex.getMessage());
        } catch (IOException ex) {
            // report a StreamerCapacity.Status.ONHOLD
            logger.debug("Network error while connecting to {}; {}",
                    urlStr, ex.getMessage());
            // no need to log that error (only for debugging)
        }
        
        if (!healthy) {
            // resize lists, if too short
            if (maxAge.size() < i+1) {
                int oldSize = maxAge.size();
                for (int j = oldSize; j < i+1; j++) {
                    maxAge.add(0L);
                    onHold.add(false);
                }
            }
            // set the info
            if (sinceWhen == Instant.MAX) {
                sinceWhen = Instant.now();
            }
            if (lastModSince == 0) {
                // means that some exceptions was caught
                maxAge.set(i, 999999L);
            } else if (maxAge.get(i) < lastModSince) {
                maxAge.set(i, lastModSince);
            }
            onHold.set(i, true);
        }
        
        return healthy;
    }
    
    // TODO: if stream not available at all, set StreamerCapacity.Status.NOTREADY
    private void doTheLoop() {
        
        // reload streamer capacities from csv file
        streamerInfo.updateStreamersFromFile();
        
        // for all found streamers ...
        boolean allHealthy = true;
        Object[] streamers = streamerInfo.getCopyOfStreamerCapList();
        // classical for-loop, because I need the index i
        for (int i = 0; i < streamers.length; i++) {
            StreamerCapacity cap = (StreamerCapacity)(streamers[i]);
            String protocol = "https";
            String resource = "/play/livestream.m3u8";
            String urlStr = protocol + "://" + cap.getHostname() + resource;
            if (!liveStreamHealthy(urlStr, i)) {
                cap.setStatus(StreamerCapacity.Status.ONHOLD);
                allHealthy = false;
            } else {
                cap.setStatus(StreamerCapacity.Status.WORKING);
            }
        }
        
        if ((allHealthy) && (sinceWhen != Instant.MAX)) {
            // this means that they're healthy now,
            // but some recordings were gathered
            String statsFilename = "/var/log/tomcat9/health_incidents.csv";
            StringBuilder statsLine = new StringBuilder();
            statsLine.append(sinceWhen.toString()).append(", ");
            // onHold isn't needed, since healthy streamers have maxAge 0
            maxAge.forEach((age) -> statsLine.append(age/1000.0).append(", "));
            try {
                File statsFile = new File(statsFilename);
                FileUtils.writeStringToFile(statsFile,
                        statsLine.append(System.lineSeparator()).toString(),
                        Charset.forName("UTF-8"),
                        true); // append
            } catch (IOException ex) {
                logger.error(ex.getMessage());
                logger.error("Could not write stats to {}", statsFilename);
            }
            
            // reset everything (esp. sinceWhen = Instant.MAX)
            resetHealthRecordings(streamers.length);
        }
    }
    
    @Override
    public void preStart() {
        logger.debug("preStart()");
        getTimers().startTimerAtFixedRate(TIMERKEY, new HealthCheckLoop(), LOOPWAIT);
    }

    @Override
    public void postStop() {
        logger.debug("postStop()");
        getTimers().cancel(TIMERKEY);
    }

    @Override
    public AbstractActor.Receive createReceive() {
        return actorReceiveBlt.build();
    }
    
}
