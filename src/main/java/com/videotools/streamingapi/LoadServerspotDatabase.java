package com.videotools.streamingapi;

import com.videotools.streamingapi.model.ServerspotRepository;
import com.videotools.streamingapi.model.Serverspot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class LoadServerspotDatabase {

    private static final Logger log = LoggerFactory.getLogger(LoadServerspotDatabase.class);

    @Bean
    public CommandLineRunner initServerspotDatabase(ServerspotRepository repository) {

        return args -> {
            log.info("Preloading " + repository.save(new Serverspot("Bilbo Serverspot", "burglar")));
            log.info("Preloading " + repository.save(new Serverspot("Frodo Serverspot", "thief")));
        };
    }

}
