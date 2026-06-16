package com.example.medicalmcp.dataset.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DatasetLoaderServiceImplTest {

    @Test
    void inferSplitFromTrainFile() {
        assertThat(DatasetLoaderServiceImpl.inferSplit("classpath:dataset/train-sample-10.csv"))
                .isEqualTo("train");
        assertThat(DatasetLoaderServiceImpl.inferSplit("medical_cases_validation.csv"))
                .isEqualTo("validation");
        assertThat(DatasetLoaderServiceImpl.inferSplit("medical_cases_test.csv")).isEqualTo("test");
    }
}
