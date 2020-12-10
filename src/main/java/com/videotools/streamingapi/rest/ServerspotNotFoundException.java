package com.videotools.streamingapi.rest;


public class ServerspotNotFoundException extends RuntimeException {

    public ServerspotNotFoundException(Long id) {
        super("Could not find serverspot " + id);
    }

}
