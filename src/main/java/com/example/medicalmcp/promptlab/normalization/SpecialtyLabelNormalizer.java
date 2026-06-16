package com.example.medicalmcp.promptlab.normalization;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.util.StringUtils;

public final class SpecialtyLabelNormalizer {

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

    private static final Map<String, String> ALIASES = buildAliases();

    private SpecialtyLabelNormalizer() {}

    public static Optional<String> toCanonical(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (CANONICAL_LABELS.contains(trimmed)) {
            return Optional.of(trimmed);
        }
        String key = normalizeKey(trimmed);
        return Optional.ofNullable(ALIASES.get(key));
    }

    public static String toSnakeCase(String canonicalLabel) {
        return canonicalLabel
                .toLowerCase(Locale.ROOT)
                .replace(" - ", "_")
                .replace(" / ", "_")
                .replace(" ", "_")
                .replaceAll("_+", "_");
    }

    private static Map<String, String> buildAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        for (String label : CANONICAL_LABELS) {
            aliases.put(normalizeKey(label), label);
            aliases.put(normalizeKey(toSnakeCase(label)), label);
        }
        aliases.put("cardiology", "Cardiovascular / Pulmonary");
        aliases.put("obstetrics_and_gynecology", "Obstetrics / Gynecology");
        aliases.put("obstetrics_gynecology", "Obstetrics / Gynecology");
        aliases.put("ent_otolaryngology", "ENT - Otolaryngology");
        aliases.put("hematology_oncology", "Hematology - Oncology");
        aliases.put("psychiatry_psychology", "Psychiatry / Psychology");
        aliases.put("pediatrics_neonatal", "Pediatrics - Neonatal");
        return Map.copyOf(aliases);
    }

    private static String normalizeKey(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace(" - ", "_")
                .replace(" / ", "_")
                .replace(' ', '_')
                .replaceAll("_+", "_");
    }
}
