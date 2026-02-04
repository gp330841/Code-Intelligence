package com.codeintelligence.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StartupCheck implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        log.info("=================================================");
        log.info("   APPLICATION STARTED SUCCESSFULLY ON PORT 8080");
        log.info("   Swagger UI: http://localhost:8080/swagger-ui.html");
        log.info("   ChromaDB:   http://localhost:8010");
        log.info("=================================================");
    }
}
