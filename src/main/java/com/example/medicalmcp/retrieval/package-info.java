@org.springframework.modulith.ApplicationModule(allowedDependencies = {
    "core :: *",
    "medicalcase :: domain",
    "medicalcase :: repository",
    "embedding :: service"
})
package com.example.medicalmcp.retrieval;
