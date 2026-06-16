package com.example.medicalmcp.integration;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Single pgvector Postgres instance per JVM fork (failsafe {@code forkCount=1}).
 * Avoids per-test-class {@code @Container} restarts that break cached Spring contexts.
 */
final class SharedPostgresContainer {

    private static final DockerImageName PGVECTOR =
            DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres");

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>(PGVECTOR)
            .withDatabaseName("medical_mcp")
            .withUsername("medical_mcp")
            .withPassword("medical_mcp");

    static {
        INSTANCE.start();
    }

    private SharedPostgresContainer() {}
}
