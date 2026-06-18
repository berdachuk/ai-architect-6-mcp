package com.example.medicalmcp.core.util;

import java.util.UUID;
import org.springframework.util.StringUtils;

public final class UuidUtils {

    private UuidUtils() {}

    public static UUID parseUuid(String id) {
        if (!StringUtils.hasText(id)) {
            return null;
        }
        try {
            return UUID.fromString(id.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
