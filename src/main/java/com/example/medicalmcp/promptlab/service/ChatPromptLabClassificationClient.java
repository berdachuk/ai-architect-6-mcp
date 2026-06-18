package com.example.medicalmcp.promptlab.service;

import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import com.example.medicalmcp.promptlab.eval.CaseTextFormatter;
import java.util.List;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prompt-lab")
@ConditionalOnProperty(prefix = "medicalmcp.prompt-lab.chat", name = "enabled", havingValue = "true")
public class ChatPromptLabClassificationClient implements PromptLabClassificationClient {

    private final ChatModel chatModel;

    public ChatPromptLabClassificationClient(@Qualifier("promptLabChatModel") ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String classify(MedicalCase medicalCase, String systemPrompt, String templateId) {
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt), new UserMessage(CaseTextFormatter.format(medicalCase))));
        ChatResponse response = chatModel.call(prompt);
        return response.getResult().getOutput().getText();
    }
}
