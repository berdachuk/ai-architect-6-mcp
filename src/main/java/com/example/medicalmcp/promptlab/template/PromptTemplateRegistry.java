package com.example.medicalmcp.promptlab.template;

import com.example.medicalmcp.promptlab.domain.PromptTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prompt-lab")
public class PromptTemplateRegistry {

    private final Map<String, PromptTemplate> custom = new ConcurrentHashMap<>();

    public Optional<PromptTemplate> findById(String id) {
        return Optional.ofNullable(custom.get(id)).or(() -> PromptTemplateLibrary.findById(id));
    }

    public void register(String id, String name, String systemText) {
        custom.put(id, new PromptTemplate(id, name, systemText));
    }

    public List<PromptTemplate> all() {
        List<PromptTemplate> merged = new ArrayList<>(PromptTemplateLibrary.all());
        merged.addAll(custom.values());
        return List.copyOf(merged);
    }
}
