package com.videotools.streamingapi.actors;

import akka.actor.ActorRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ActorAdminBean implements CommandLineRunner {

    @Override
    public void run(String... args) {
        
        Logger logger
            = LoggerFactory.getLogger(ActorAdminBean.class);
        
        logger.debug("Starting the ActorSystem ...");
        ActorRef statsActor = ActorSingleton.getInstance().getStatsActor();
        statsActor.tell(new LeaseStatsActor.AreYouResponsive(),
                ActorRef.noSender());
        logger.debug("ActorSystem started");
    }
    
}
