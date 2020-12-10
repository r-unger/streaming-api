package com.videotools.streamingapi.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
class LicenseNotFoundAdvice {

    @ResponseBody
    @ExceptionHandler(LicenseNotFoundException.class) // when LicenseNotFound
    @ResponseStatus(HttpStatus.NOT_FOUND)              // set HTTP status 404
    String licenseNotFoundHandler(LicenseNotFoundException ex) {
        return ex.getMessage();                        // and the error message
    }

}
