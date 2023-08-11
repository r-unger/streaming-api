package com.videotools.streamingapi.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.videotools.streamingapi.model.SyncedStreamerInfo;

public class ActorSingleton {
    
    // thread-safe implementation from
    // https://de.wikibooks.org/wiki/Muster:_Java:_Singleton
    
    // Have this instance hidden
    private static ActorSingleton instance;
    // Hide the constructor as well
    // (so it can only be called from within this class)
    private ActorSingleton () {
        actorSystem = ActorSystem.create("streaming-api");
        statsActor = actorSystem.actorOf(
                LeaseStatsActor.props(),
                "LeaseStatsActor");
        healthCheckActor = actorSystem.actorOf(
                HealthCheckActor.props(),
                "HealthCheckActor");
        String streamerInfoFilename = "/etc/streaming-api/streamerinfo.csv";
        syncedStreamerInfo = new SyncedStreamerInfo(streamerInfoFilename);
    }
    // This implementation of static getInstance() makes sure that
    // the object is only created '''once'''.
    public static synchronized ActorSingleton getInstance () {
        if (ActorSingleton.instance == null) {
            ActorSingleton.instance = new ActorSingleton ();
        }
        return ActorSingleton.instance;
    }
    
    // The encapsulated objects
    private final ActorSystem actorSystem;
    private final ActorRef statsActor;
    private final ActorRef healthCheckActor;
    private final SyncedStreamerInfo syncedStreamerInfo;

    // with their getter methods
    public ActorSystem getActorSystem() {
        return actorSystem;
    }

    public ActorRef getStatsActor() {
        return statsActor;
    }
    
    public SyncedStreamerInfo getSyncedStreamerInfo() {
        return syncedStreamerInfo;
    }
    // It's important not to have setters

}
