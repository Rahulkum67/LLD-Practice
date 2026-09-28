package com.lld;

public interface LlmClient {
    String complete(String system, String prompt);
}
