package org.example.sds_project.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TranscriptionFactory {
    private final Map<String, TranscriptionStrategy> strategies;

    public TranscriptionFactory(List<TranscriptionStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(TranscriptionStrategy::getProviderName, strategy -> strategy));
    }

    public TranscriptionStrategy getStrategy(String providerName) {
        if (providerName == null || !strategies.containsKey(providerName.toLowerCase())) {
            return strategies.get("gemini");
        }
        return strategies.get(providerName.toLowerCase());
    }
}
