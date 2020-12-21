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
import com.videotools.streamingapi.model.Serverspot;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author xyz
 */
public class LeaseStatsActor extends AbstractActorWithTimers {

    // Props to create the actor
    public static Props props() {
        return Props.create(LeaseStatsActor.class, () -> new LeaseStatsActor());
    }

    // Actor messages
    static public class AreYouResponsive {
        public AreYouResponsive() {
        }
    }
    static public class RegisterServerspot {
        private final Serverspot serverspot;
        private final String ipAddress;
        public RegisterServerspot(Serverspot serverspot, String ipAddress) {
            this.serverspot = serverspot;
            this.ipAddress = ipAddress;
        }
    }
    static public class AddDataPoint {
        // members could be just License, but let's start with the token only
        private final int token;
        // int playTimeInSecs;  - can be calculated
        public AddDataPoint(int token) {
            this.token = token;
        }
    }
// TODO: update the leaseTime; either correct doTheStatsLoop() or just report in that same frequency
//          letting grafana do the average
// TODO: report current workload for /serverspots; perhaps in another actor with its own mail queue
//          new idea: have that map as a concurrent & singleton, so that the ReST servlet doesn't have
//          to wait on message processing
    static public class StatsLoop {
        public StatsLoop() {
        }
    }
    // Actor messages

    private final Object TIMERKEY;
    private final Duration LOOPWAIT;
    
    private final LoggingAdapter logger;
    protected final ReceiveBuilder actorReceiveBlt;
    
    private int counter; // starter tests only
    private LeaseStatsData leaseStatsData;

    protected LeaseStatsActor() {
        
        logger = Logging.getLogger(getContext().getSystem(), this);
        counter = 0;
        
        TIMERKEY = getSelf().path().name() + " Loop";
        LOOPWAIT = Duration.of(60, ChronoUnit.SECONDS);
        
        leaseStatsData = new LeaseStatsData();
        
        actorReceiveBlt =
                receiveBuilder()
                .match(AreYouResponsive.class, areYouResponsive -> {
                    counter++;
                    logger.debug("Got request 'Are you responsive?'; counter is {}", counter);
                })
                .match(RegisterServerspot.class, register -> {
                    logger.debug(
                            "Got request 'RegisterServerspot' for {}, token {}",
                            register.serverspot.getHostname(),
                            register.serverspot.getToken());
                    leaseStatsData.registerServerspot(register.serverspot, register.ipAddress);
                })
                .match(AddDataPoint.class, dataPoint -> {
                    logger.debug(
                            "Got request 'DataPoint' for token {}",
                            dataPoint.token);
                    leaseStatsData.addDataPoint(dataPoint.token);
                })
                .match(StatsLoop.class, doTheStatsLoop -> {
                    logger.debug("Got request 'DoTheStatsLoop'");
                    doTheStatsLoop();
                });
    }

    private void doTheStatsLoop() {
        
        // remove old (stale) leases)
        leaseStatsData.removeStaleLeases();
        // (perhaps put them on hold for x days; or save them to file)
// TODO: to avoid very old lease renewals, the lease must reclaim a new serverspot after some elapsed time
        
        // calc. the stats
        String statsFilename = "/var/log/streaming-api/stats_table.txt";
        StringBuilder statsLine = new StringBuilder();
        statsLine.append(LocalTime.now().toString()).append(" - ");
// group?
// 2 kinds of stats:
// workload incl. those what reserved, but didn't .play
// usage only those with more than a minute playtime
        leaseStatsData.getServerWorkload().forEach((hostname, count)
                -> statsLine.append(hostname).append(": ")
                            .append(count).append(", "));
        statsLine.append("removed but active: ").append(leaseStatsData.getRemovedLeaseCounter());
        leaseStatsData.resetRemovedLeaseCounter();
        // statsLine.append(", free mem: ").append(Runtime.getRuntime().freeMemory()/1024/1024).append(" MiB");
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
    }
    
    @Override
    public void preStart() {
        logger.debug("preStart()");
        getTimers().startTimerAtFixedRate(TIMERKEY, new StatsLoop(), LOOPWAIT);
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
