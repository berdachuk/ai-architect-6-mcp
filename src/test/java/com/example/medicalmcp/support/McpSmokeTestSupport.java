package com.example.medicalmcp.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class McpSmokeTestSupport {

    private McpSmokeTestSupport() {}

    public static void assertToolSuccess(CallToolResult result) {
        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
    }

    public static JsonNode toJson(CallToolResult result, ObjectMapper objectMapper) {
        assertToolSuccess(result);
        try {
            Object payload = result.structuredContent();
            if (payload instanceof String text && StringUtils.hasText(text)) {
                return objectMapper.readTree(text);
            }
            if (payload != null) {
                JsonNode node = objectMapper.valueToTree(payload);
                if (node.isTextual()) {
                    return objectMapper.readTree(node.asText());
                }
                return node;
            }
            if (result.content() != null && !result.content().isEmpty()) {
                return objectMapper.readTree(textFromContent(result.content().getFirst()));
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse MCP tool result", ex);
        }
        throw new AssertionError("No parseable MCP tool payload");
    }

    public static String textFromResource(ReadResourceResult result) {
        assertThat(result.contents()).isNotEmpty();
        var content = result.contents().getFirst();
        assertThat(content).isInstanceOf(TextResourceContents.class);
        return ((TextResourceContents) content).text();
    }

    private static String textFromContent(Content content) {
        assertThat(content).isInstanceOf(TextContent.class);
        return ((TextContent) content).text();
    }

    public static Map<String, Object> args(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Expected key/value pairs");
        }
        java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return Map.copyOf(map);
    }
}
