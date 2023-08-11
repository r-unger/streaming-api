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
// http://api.thecompany.com/streaming-api-v1/licenses
// (but any client should follow links anyway)

import akka.actor.ActorRef;
import com.videotools.streamingapi.actors.ActorSingleton;
import com.videotools.streamingapi.actors.LeaseStatsActor;
import com.videotools.streamingapi.model.LicenseRepository;
import com.videotools.streamingapi.model.License;
import jakarta.servlet.http.HttpServletRequest;

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
public class LicenseController {
        // @RestController indicates that the data returned by each method
        // will be written straight into the response body instead of
        // rendering a template

    private final LicenseModelAssembler assembler;

    LicenseController(
            LicenseModelAssembler assembler) {

        this.assembler = assembler;
    }

    // Aggregate root

    @GetMapping("/licenses")
    CollectionModel<EntityModel<License>> all() {

        throw new ResourceForbiddenException("/licenses/");
    }

    // correct call:
    // curl -X POST -H 'Content-Type: application/json' -d '{}'
    //'https://api.thecompany.com/streaming-api-v1/licenses?token=23&asset=file'
    @PostMapping("/licenses")
    ResponseEntity<?> newLicense(
            @RequestParam(value="token", defaultValue="", required=true) Integer token,
            @RequestParam(value="asset", defaultValue="") String asset,
            HttpServletRequest request) {
        
        String status = "";
        int leaseTime = 60000;
        
        License newLicense = new License(token, asset, status, leaseTime);

        // inform the statsActor
        ActorRef statsActor = ActorSingleton.getInstance().getStatsActor();
        statsActor.tell(new LeaseStatsActor.AddDataPoint(token),
                ActorRef.noSender());

        EntityModel<License> entityModel =
                assembler.toModel(newLicense);

        return ResponseEntity
                .created(entityModel
                        .getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(entityModel);
    }
    /* Response body:
        HTTP/1.1 201 (created)
        Location: https://api.thecompany.com/streaming-api-v1/licenses/4
        Content-Type: application/hal+json
    */

    // Single item

    @GetMapping("/licenses/{token}")
    EntityModel<License> one(@PathVariable Integer token) {

        throw new ResourceForbiddenException("/licenses/" + token);
    }

    // correct call:
    //  curl -X PUT -H 'Content-Type: application/json' -d
    // '{"token":234,"asset":"file","status":"","leaseTime":60000}'
    // 'https://api.thecompany.com/streaming-api-v1/licenses/234'
    @PutMapping("/licenses/{token}")
    ResponseEntity<?> updateLicense(
            @RequestBody License newLicense,
            @PathVariable Integer token) {

        License updatedLicense = newLicense;

        // inform the statsActor
        ActorRef statsActor = ActorSingleton.getInstance().getStatsActor();
        statsActor.tell(new LeaseStatsActor.AddDataPoint(token),
                ActorRef.noSender());

        EntityModel<License> entityModel =
                assembler.toModel(updatedLicense);

        return ResponseEntity
                .created(entityModel
                        .getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(entityModel);
    }
    /* same response body as with newLicense() above */

    @DeleteMapping("/licenses/{token}")
    ResponseEntity<?> deleteLicense(@PathVariable Integer token) {

        throw new ResourceForbiddenException("/licenses/" + token);
    }
    /* Response body:
        HTTP/1.1 204 (no content; which means:
        "The server has successfully fulfilled the request and that there
        is no additional content to send in the response payload body.")
    */

}
