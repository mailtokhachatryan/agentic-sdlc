package com.agenticdev.sdlc.llm;

public class ProviderNotConfiguredException extends RuntimeException {
    private final Provider provider;

    public ProviderNotConfiguredException(Provider provider) {
        super(provider + " is not configured");
        this.provider = provider;
    }

    public Provider provider() { return provider; }
}
