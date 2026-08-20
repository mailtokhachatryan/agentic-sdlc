package com.agenticdev.sdlc.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class LlmCallMetrics {

    private final MeterRegistry registry;

    public LlmCallMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordCall(String provider, String model, Duration duration) {
        Timer.builder("llm.call.duration")
                .tags(Tags.of("provider", provider, "model", model))
                .register(registry)
                .record(duration);
    }

    public void recordPlan(String provider, String model, String inputType, String status, Duration duration) {
        Tags tags = Tags.of("provider", provider, "model", model, "inputType", inputType, "status", status);
        registry.counter("plans.created", tags).increment();
        Timer.builder("plans.duration").tags(tags).register(registry).record(duration);
    }
}
