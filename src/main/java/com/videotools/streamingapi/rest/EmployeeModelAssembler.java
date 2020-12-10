package com.videotools.streamingapi.rest;

import com.videotools.streamingapi.model.Employee;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component  // by applying Spring Framework’s @Component, this component
            // will be automatically created when the app starts
class EmployeeModelAssembler
        implements RepresentationModelAssembler
                <Employee, EntityModel<Employee>> {

    @Override
    public EntityModel<Employee> toModel(Employee employee) {

        return EntityModel.of(employee,
                linkTo(methodOn(EmployeeController.class)
                        .one(employee.getId())).withSelfRel(),
                linkTo(methodOn(EmployeeController.class)
                        .all()).withRel("employees"));
    }

}
