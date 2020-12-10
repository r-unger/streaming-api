package com.videotools.streamingapi.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;


@ControllerAdvice
public class ServerspotNotFoundAdvice {

    @ResponseBody
    @ExceptionHandler(ServerspotNotFoundException.class) // when ServerspotNotFound
    @ResponseStatus(HttpStatus.NOT_FOUND)                // set HTTP status 404
    String serverspotNotFoundHandler(ServerspotNotFoundException ex) {
        return ex.getMessage();                          // and the error message
    }

}
