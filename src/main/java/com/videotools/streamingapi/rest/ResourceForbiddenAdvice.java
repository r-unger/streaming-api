package com.videotools.streamingapi.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;


@ControllerAdvice
public class ResourceForbiddenAdvice {

    @ResponseBody
    @ExceptionHandler(ResourceForbiddenException.class) // when ResourceForbidden
    @ResponseStatus(HttpStatus.FORBIDDEN)               // set HTTP status 403
    String resourceForbiddenHandler(ResourceForbiddenException ex) {
        return ex.getMessage();                         // and the error message
    }

}
