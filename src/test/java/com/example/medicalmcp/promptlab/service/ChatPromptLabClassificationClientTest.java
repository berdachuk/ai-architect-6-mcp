package com.example.medicalmcp.promptlab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.medicalmcp.medicalcase.domain.MedicalCase;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

@ExtendWith(MockitoExtension.class)
class ChatPromptLabClassificationClientTest {

    @Mock
    private ChatModel chatModel;

    @Test
    void classifySendsSystemAndUserMessages() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("PREDICTED_LABEL: cardiology")))));

        ChatPromptLabClassificationClient client = new ChatPromptLabClassificationClient(chatModel);
        MedicalCase medicalCase = new MedicalCase(
                UUID.randomUUID(),
                "Case A",
                "Cardiology case",
                "Patient chest pain",
                "Cardiovascular / Pulmonary",
                "chest pain",
                "validation",
                Instant.now());

        String output = client.classify(medicalCase, "Classify specialty", "react_self_reflection");

        assertThat(output).isEqualTo("PREDICTED_LABEL: cardiology");

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        Prompt prompt = promptCaptor.getValue();
        assertThat(prompt.getInstructions()).hasSize(2);
        assertThat(prompt.getInstructions().get(0)).isInstanceOf(SystemMessage.class);
        assertThat(((SystemMessage) prompt.getInstructions().get(0)).getText()).isEqualTo("Classify specialty");
        assertThat(prompt.getInstructions().get(1)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) prompt.getInstructions().get(1)).getText()).contains("Case A");
    }
}
