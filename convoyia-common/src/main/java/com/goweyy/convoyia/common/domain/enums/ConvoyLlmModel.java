package com.goweyy.convoyia.common.domain.enums;

public enum ConvoyLlmModel {
    PHI3_MINI("phi3:mini"),
    MISTRAL_7B("mistral:7b-instruct"),
    LLAMA3_8B("llama3:8b"),
    QWEN_VL_7B("qwen:7b"),
    CLAUDE_SONNET("claude-sonnet-4-6");

    private final String tag;

    ConvoyLlmModel(String tag) {
        this.tag = tag;
    }

    public String getTag() { return tag; }
}
