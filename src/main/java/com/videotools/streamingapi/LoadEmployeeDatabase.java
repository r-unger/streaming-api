package com.videotools.streamingapi;

import com.videotools.streamingapi.model.EmployeeRepository;
import com.videotools.streamingapi.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class LoadEmployeeDatabase {

    private static final Logger log = LoggerFactory.getLogger(LoadEmployeeDatabase.class);

    @Bean
    public CommandLineRunner initEmployeeDatabase(EmployeeRepository repository) {

        return args -> {
            log.info("Preloading " + repository.save(new Employee("Bilbo Baggins", "burglar")));
            log.info("Preloading " + repository.save(new Employee("Frodo Baggins", "thief")));
        };
    }

}
