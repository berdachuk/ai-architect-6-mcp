package com.example.medicalmcp.embedding.service;

import java.util.List;

public interface EmbeddingService {

    float[] embedAsFloatArray(String text);

    List<float[]> embedBatch(List<String> texts);

    String buildEmbeddingInput(String sampleName, String description, String keywords);

    List<EmbeddingHealth> pingAll();
}
