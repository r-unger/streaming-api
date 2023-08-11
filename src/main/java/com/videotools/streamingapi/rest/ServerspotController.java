package com.videotools.streamingapi.rest;

/*
 * Code based on the Spring tutorial
 * "Building REST services with Spring"
 * https://spring.io/guides/tutorials/rest/
 * and
 * https://github.com/spring-guides/tut-rest
*/
// TODO: Links are working, even with Apache httpd as proxy,
// but they show http instead of https
// http://api.thecompany.com/streaming-api-v1/serverspots
// (but any client should follow links anyway)

import akka.actor.ActorRef;
import com.videotools.streamingapi.actors.ActorSingleton;
import com.videotools.streamingapi.actors.LeaseStatsActor;
import com.videotools.streamingapi.model.Serverspot;
import com.videotools.streamingapi.model.ServerspotRepository;
import com.videotools.streamingapi.model.StreamerCapacity;

import java.util.concurrent.ThreadLocalRandom;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
public class ServerspotController {
        // @RestController indicates that the data returned by each method
        // will be written straight into the response body instead of
        // rendering a template

    // the RequestBody for POST /serverspots
    public class PostServerspotRequest {
        private String group;
        public PostServerspotRequest() {
        }
        public String getGroup() {
            return group;
        }
        public void setGroup(String group) {
            this.group = group;
        }
    }

    private static final org.slf4j.Logger logger
            = LoggerFactory.getLogger(ServerspotController.class);
    
    private final ServerspotModelAssembler assembler;

    ServerspotController(
            ServerspotModelAssembler assembler) {

        this.assembler = assembler;
    }

    // Aggregate root

    @GetMapping("/serverspots")
    CollectionModel<EntityModel<Serverspot>> all() {

        throw new ResourceForbiddenException("/serverspots/");
    }

    private String drawHostname(ThreadLocalRandom rand) {
        
        // get the capacity
        // (perhaps save a timestamp so that the capacity is only updated once a minute)
        // same with current workload (once in a while or each time)
        
        String hostname = null;
        int totalWorkload = 20;
        /* Sample data for quick testing:
        StreamerCapacity pm1 = new StreamerCapacity("www1.thecompany.com", 100, 140, 3);
                                    // subtracted must be the no. of streamer instances
        StreamerCapacity pm2 = new StreamerCapacity("www2.thecompany.com", 100, 150, 3);
            // 2 because the library plays are not yet counted
            // www3 is the old server with outdated programs
        StreamerCapacity sat1 = new StreamerCapacity("www3.thecompany.com", 50, 100, 2);
        StreamerCapacity sat2 = new StreamerCapacity("www5.thecompany.com", 50, 100, 2);
        StreamerCapacity pa1 = new StreamerCapacity("www4.thecompany.com", 50, 100, 2);
            // a bit less than the other cloud instances, because of SQL, influxDB, grafana
        StreamerCapacity[] streamers = new StreamerCapacity[]
                {pm1, pm2, sat1, sat2, pa1 };
        */
        Object[] streamers = ActorSingleton.getInstance()
                .getSyncedStreamerInfo()
                .getCopyOfStreamerCapList();
        
        int totalCapacity = 0;
        int weightedCapacity = 0;
        for (Object o : streamers) {
            StreamerCapacity cap = (StreamerCapacity)o;
            logger.debug("StreamerCapacity: {}", cap.toString());
            if (cap.getStatus() == StreamerCapacity.Status.WORKING) {
                totalCapacity += cap.getHundredPercentCapacity();
                weightedCapacity +=
                        cap.getHundredPercentCapacity() * cap.getPriority();
            }
        }

        // if workload reaches 80% of the nominal capacity
        if (totalWorkload >= totalCapacity*0.8) {
            // ...
            // send email etc.
        }

        if (weightedCapacity > 0) {
            int spotNo = rand.nextInt(weightedCapacity) + 1; // [1..n]

            int counter = 0;
            for (Object o: streamers) {
                StreamerCapacity cap = (StreamerCapacity)o;
                // IMPT: this loop must use the same logic as the loop above
                if (cap.getStatus() == StreamerCapacity.Status.WORKING) {
                    counter +=
                            cap.getHundredPercentCapacity() * cap.getPriority();
                    if (counter >= spotNo) {
                        hostname = cap.getHostname();
                        break;
                    }
                }
            }
        }
        // if (weightedCapacity == 0),
        // there is no streamer configured, or all are overloaded
        
        if (hostname == null) {
            // should only happen when
            // the array of StreamerCapacities is empty
            // hostname = ... default from .properties file?
        }
        
        return hostname;
    }
    
    private static String getIpAddress(HttpServletRequest request) {
        String ipAddress = null;
        //ipAddress = request.getRemoteAddr();
            // 0:0:0:0:0:0:0:1
            // does not work, because Apache httpd forwards to tomcat
        ipAddress = request.getHeader("X-FORWARDED-FOR");
            // works, but this may be a comma-seperated list where every proxy
            // on the way adds its source
            // if yes, then just get the right-most address after the last comma
        // other implementation (seems to be complete):
        // https://stackoverflow.com/questions/22877350/how-to-extract-ip-address-in-spring-mvc-controller-get-call
        // (look at 3rd answer)
        
        if (ipAddress == null) {
            ipAddress = "";
        }
        
        return ipAddress;
    }
    
    // unfortunately modSecurity is blocking a POST without RequestBody:
    // "POST request missing Content-Length Header."
    // Best work-around is to send an empty json:
    // curl -X POST -H "Content-Type: application/json" -d '{}'
    // 'https://api.thecompany.com/streaming-api-v1/serverspots?group=xyz'
    @PostMapping("/serverspots")
    ResponseEntity<?> newServerspot(@RequestParam(value="group", defaultValue="")
            String group, HttpServletRequest request) {

        ThreadLocalRandom rand = ThreadLocalRandom.current();
            // ThreadLocalRandom means: each thread has its own random object
            // better performance than a shared random object between threads

        String hostname;
        
        switch (group) {
            case "live.eftv":
                hostname = drawHostname(rand);
                break;
            case "live.adopl":
                hostname = drawHostname(rand);
                break;
            case "play.eftv":
                hostname = "www2.thecompany.com";
                break;
            default:
                hostname = "thecompany.com";
                // if no group is given, return the main host;
                // but perhaps it's better to throw an error here,
                // making the group mandatory
        }
        
        if (hostname == null) {
            // may happen, if no server capacities are found
            // (configuration error or fresh system?)
            hostname = "thecompany.com";
        }
        int token = Math.abs(rand.nextInt());
        
        Serverspot newServerspot = new Serverspot(
                hostname, group, token);

        String ipAddress = getIpAddress(request);
        
        // register the serverspot for the stats
        ActorRef statsActor = ActorSingleton.getInstance().getStatsActor();
        statsActor.tell(new LeaseStatsActor.RegisterServerspot(newServerspot, ipAddress),
                ActorRef.noSender());

        EntityModel<Serverspot> entityModel =
                assembler.toModel(newServerspot);

        return ResponseEntity
                .created(entityModel
                        .getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(entityModel);
    }
    /* Response body:
        HTTP/1.1 201 (created)
        Location: https://api.thecompany.com/streaming-api-v1/serverspots/4
        Content-Type: application/hal+json
    */

    // Single item

    @GetMapping("/serverspots/{token}")
    EntityModel<Serverspot> one(@PathVariable Integer token) {

        throw new ResourceForbiddenException("/serverspots/" + token);
    }

    @PutMapping("/serverspots/{token}")
    ResponseEntity<?> replaceServerspot(
            @RequestBody Serverspot newServerspot,
            @PathVariable Integer token) {
            // replace is a better description than update; e.g. if the name
            // was NOT provided, it would instead get nulled out

        throw new ResourceForbiddenException("/serverspots/" + token);
    }
    /* same response body as newServerspot() above */

    @DeleteMapping("/serverspots/{token}")
    ResponseEntity<?> deleteServerspot(@PathVariable Integer token) {

        throw new ResourceForbiddenException("/serverspots/" + token);
    }
    /* Response body:
        HTTP/1.1 204 (no content; which means:
        "The server has successfully fulfilled the request and that there
        is no additional content to send in the response payload body.")
    */

}
