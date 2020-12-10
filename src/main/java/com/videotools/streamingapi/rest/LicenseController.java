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
// http://api.eucharisticflame.tv/streaming-api-v1/licenses
// (but any client should follow links anyway)

import com.videotools.streamingapi.model.LicenseRepository;
import com.videotools.streamingapi.model.License;
import java.util.List;
import java.util.stream.Collectors;

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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import org.springframework.http.ResponseEntity;


@RestController
public class LicenseController {
        // @RestController indicates that the data returned by each method
        // will be written straight into the response body instead of
        // rendering a template

    private final LicenseRepository repository;

    private final LicenseModelAssembler assembler;

    LicenseController(
            LicenseRepository repository,
            LicenseModelAssembler assembler) {

        // the LicenseRepository and the Assembler are injected
        // by constructor into the controller
        this.repository = repository;
        this.assembler = assembler;
    }

    // Aggregate root

    @GetMapping("/licenses")
    CollectionModel<EntityModel<License>> all() {

        List<EntityModel<License>> licenses =
                repository
                        .findAll()
                        .stream()
                        .map(assembler::toModel)
                        .collect(Collectors.toList());

        return CollectionModel.of(licenses,
                linkTo(methodOn(LicenseController.class)
                        .all()).withSelfRel());
    }

    @PostMapping("/licenses")
    ResponseEntity<?> newLicense(@RequestBody License newLicense) {

        EntityModel<License> entityModel =
                assembler.toModel(repository.save(newLicense));

        return ResponseEntity
                .created(entityModel
                        .getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(entityModel);
    }
    /* Response body:
        HTTP/1.1 201 (created)
        Location: http://localhost:8080/licenses/4
        Content-Type: application/hal+json
    */
    // but better would be: Content-Type: application/hal+json;charset=UTF-8

    // Single item

    @GetMapping("/licenses/{id}")
    EntityModel<License> one(@PathVariable Long id) {

        License license = repository.findById(id)
                .orElseThrow(() -> new LicenseNotFoundException(id));

        return assembler.toModel(license);
    }

    @PutMapping("/licenses/{id}")
    ResponseEntity<?> replaceLicense(
            @RequestBody License newLicense,
            @PathVariable Long id) {
            // replace is a better description than update; e.g. if the name
            // was NOT provided, it would instead get nulled out

        License updatedLicense = repository.findById(id)
                .map(license -> {
                    license.setName(newLicense.getName());
                    license.setRole(newLicense.getRole());
                    return repository.save(license);
                })
                .orElseGet(() -> {
                    newLicense.setId(id);
                    return repository.save(newLicense);
                });
                // Remark: A PUT with an unknown id still saves that record,
                // but discards the id. Instead, an incremented id is issued.
                // curl -X PUT localhost:8080/licenses/98
                // -H 'Content-type:application/json'
                // -d '{"id": 98, "name": "S. G.", "role": "other"}'
                // gives the record the next available id, n+1 (and not 98)

        EntityModel<License> entityModel = assembler.toModel(updatedLicense);

        return ResponseEntity
                .created(entityModel
                        .getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(entityModel);
    }
    /* same response body as newLicense() above */

    @DeleteMapping("/licenses/{id}")
    ResponseEntity<?> deleteLicense(@PathVariable Long id) {

        repository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
    /* Response body:
        HTTP/1.1 204 (no content; which means:
        "The server has successfully fulfilled the request and that there
        is no additional content to send in the response payload body.")
    */

}
