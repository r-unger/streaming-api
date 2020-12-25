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
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    static public class AreYouResponsive {
        public AreYouResponsive() {
        }
    }
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
    
    protected HealthCheckActor() {
        
        logger = Logging.getLogger(getContext().getSystem(), this);
        
        TIMERKEY = getSelf().path().name() + " Loop";
        LOOPWAIT = Duration.of(10, ChronoUnit.SECONDS);
            // since several timeouts will let the loop duration be beyond
            // 10 seconds, the messages may have to wait in the queue
        
        streamerInfo = ActorSingleton.getInstance()
                .getSyncedStreamerInfo();
        
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

    private void loadStreamerCapacities() {
        
        // TODO: later from mySQL DB, ReST API, ...?
        // as of now, from csv file in the format
        // www1.eucharisticflame.tv, 100, 140, 3
        
        // plus: check stream on nfsshare!
        // IMPT: when run on several servers, have some communication
        // prdmain1 should do all the health checks (since it can access nfs.../me)
        // and report them also to prdmainX
        
    }
    
    private boolean liveStreamHealthy(String hostname) {
        
        boolean healthy = false;
        try {
            String protocol = "https";
            String resource = "/play/livestream.m3u8";
            
            URL url = new URL(protocol, hostname, resource);
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
                long dur = Instant.now().toEpochMilli() - lastModified;
                if (dur < 0) {
                    logger.error("lastModified is in the future");
                } else if (dur > 24*60*60*1000) {
                    logger.error("lastModified is in more than a day old");
                }
                
                if (dur < 15000) { // < 15 seconds
                    healthy = true;
                } else {
                    logger.warning("Resource {} is {} old",
                            url.toExternalForm(), Duration.ofMillis(dur).toString());
                }
            } else {
                // some server error, possibly 404 (not found)
                // report a StreamerCapacity.Status.NOTREADY
                logger.debug("URL '" + url.toExternalForm()
                        + "' connection with HTTP error code " + httpCode);
            }
        } catch (MalformedURLException ex) {
            logger.error("URL for accessing {} is not correct; {}",
                    hostname, ex.getMessage());
        } catch (IOException ex) {
            // report a StreamerCapacity.Status.ONHOLD
            logger.debug("Network error while connecting to {}; {}",
                    hostname, ex.getMessage());
            // no need to log that error (only for debugging)
        }
        
        return healthy;
    }
    
    // TODO: if stream not available at all, set StreamerCapacity.Status.NOTREADY
    private void doTheLoop() {
        
        // reload streamer capacities from csv file
        streamerInfo.updateStreamersFromFile();
        
        // for all found streamers ...
        Object[] streamers = streamerInfo.getCopyOfStreamerCapList();
        for (Object o: streamers) {
            StreamerCapacity cap = (StreamerCapacity)o;
            if (!liveStreamHealthy(cap.getHostname())) {
                cap.setStatus(StreamerCapacity.Status.ONHOLD);
            } else {
                cap.setStatus(StreamerCapacity.Status.WORKING);
            }
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
