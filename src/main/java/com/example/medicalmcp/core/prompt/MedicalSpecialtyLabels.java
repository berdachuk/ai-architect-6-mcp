package com.example.medicalmcp.core.prompt;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class MedicalSpecialtyLabels {

    public static final Set<String> CANONICAL_LABELS = Set.of(
            "Cardiovascular / Pulmonary",
            "Orthopedic",
            "Neurology",
            "Gastroenterology",
            "Obstetrics / Gynecology",
            "Hematology - Oncology",
            "Neurosurgery",
            "ENT - Otolaryngology",
            "Nephrology",
            "Psychiatry / Psychology",
            "Ophthalmology",
            "Pediatrics - Neonatal",
            "Radiology");

    private MedicalSpecialtyLabels() {}

    public static String toSnakeCase(String canonicalLabel) {
        return canonicalLabel
                .toLowerCase(Locale.ROOT)
                .replace(" - ", "_")
                .replace(" / ", "_")
                .replace(" ", "_")
                .replaceAll("_+", "_");
    }

    public static String allowedLabelsSnakeCase() {
        return CANONICAL_LABELS.stream().map(MedicalSpecialtyLabels::toSnakeCase).collect(Collectors.joining(", "));
    }
}
