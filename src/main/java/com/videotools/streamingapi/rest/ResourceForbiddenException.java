package com.videotools.streamingapi.rest;

public class ResourceForbiddenException extends RuntimeException {

    public ResourceForbiddenException(String resource) {
        super("Forbidden: You are not authorized to access " + resource + "\n");
    }

}
