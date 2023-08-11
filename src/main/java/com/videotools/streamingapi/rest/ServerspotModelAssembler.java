package com.videotools.streamingapi.rest;

import com.videotools.streamingapi.model.Serverspot;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;


@Component  // by applying Spring Framework’s @Component, this component
            // will be automatically created when the app starts
public class ServerspotModelAssembler
        implements RepresentationModelAssembler
                <Serverspot, EntityModel<Serverspot>> {

    @Override
    public EntityModel<Serverspot> toModel(Serverspot serverspot) {

        return EntityModel.of(serverspot,
                linkTo(methodOn(ServerspotController.class)
                        .one(serverspot.getToken())).withSelfRel());
    }

}
