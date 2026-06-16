package com.example.medicalmcp.promptlab.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "medicalmcp.prompt-lab")
public class PromptLabProperties {

    private boolean enabled;
    private String evalSplit = "validation";
    private double minAccuracy = 0.55;

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
}
