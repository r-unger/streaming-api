package com.videotools.streamingapi.rest;

import com.videotools.streamingapi.model.License;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component  // by applying Spring Framework’s @Component, this component
            // will be automatically created when the app starts
class LicenseModelAssembler
        implements RepresentationModelAssembler
                <License, EntityModel<License>> {

    @Override
    public EntityModel<License> toModel(License license) {

        return EntityModel.of(license,
                linkTo(methodOn(LicenseController.class)
                        .one(license.getToken())).withSelfRel());
    }

}
