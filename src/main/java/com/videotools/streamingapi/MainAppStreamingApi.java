package com.videotools.streamingapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MainAppStreamingApi {

    public static void main(String[] args) {
// Run locally with the embedded tomcat:
//        SpringApplication.run(MainAppStreamingApi.class, args);
// Build deployable war file:
        SpringApplication.run(GreetingController.class, args);
    }
}

