package net.noworks.clionesql.boot.samples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sample Spring Boot application demonstrating clione-sql integration.
 *
 * <p>
 * This application uses an embedded H2 database and exposes REST endpoints for CRUD operations on the {@code person}
 * table via {@link PersonController}.
 */
@SpringBootApplication
public class SampleApplication {

    /** Creates a new {@code SampleApplication}. */
    public SampleApplication() {
    }

    /**
     * Application entry point.
     *
     * @param args
     *            command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }
}
