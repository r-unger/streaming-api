package com.videotools.streamingapi;

import com.videotools.streamingapi.model.LicenseRepository;
import com.videotools.streamingapi.model.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class LoadLicenseDatabase {

    private static final Logger log = LoggerFactory.getLogger(LoadLicenseDatabase.class);

    @Bean
    public CommandLineRunner initLicenseDatabase(LicenseRepository repository) {

        return args -> {
            log.info("Preloading " + repository.save(new License("Bilbo License", "burglar")));
            log.info("Preloading " + repository.save(new License("Frodo License", "thief")));
        };
    }

}
