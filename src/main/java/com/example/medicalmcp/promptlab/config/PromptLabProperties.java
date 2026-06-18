package com.example.medicalmcp.promptlab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "medicalmcp.prompt-lab")
public class PromptLabProperties {

    private boolean enabled;
    private String evalSplit = "validation";
    private double minAccuracy = 0.55;
    private int defaultLimit = 10;
    private int maxFailureExamples = 5;
    private Chat chat = new Chat();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEvalSplit() {
        return evalSplit;
    }

    public void setEvalSplit(String evalSplit) {
        this.evalSplit = evalSplit;
    }

    public double getMinAccuracy() {
        return minAccuracy;
    }

    public void setMinAccuracy(double minAccuracy) {
        this.minAccuracy = minAccuracy;
    }

    public int getDefaultLimit() {
        return defaultLimit;
    }

    public void setDefaultLimit(int defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public int getMaxFailureExamples() {
        return maxFailureExamples;
    }

    public void setMaxFailureExamples(int maxFailureExamples) {
        this.maxFailureExamples = maxFailureExamples;
    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public static class Chat {
        private boolean enabled;
        private String baseUrl = "http://localhost:11434";
        private String model = "llama3.2";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }
}
