package com.example.medicalmcp;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithArchitectureTest {

    @Test
    void verifyModuleBoundaries() {
        ApplicationModules.of(MedicalMcpApplication.class).verify();
    }
}
