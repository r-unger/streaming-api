package com.videotools.streamingapi.rest;

public class LicenseNotFoundException extends RuntimeException {

    public LicenseNotFoundException(Long id) {
        super("Could not find license " + id);
    }

}
