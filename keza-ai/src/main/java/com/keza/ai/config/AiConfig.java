package com.keza.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for AI features.
 *
 * When keza.ai.enabled=true, Spring AI auto-configuration will handle the
 * ChatModel bean creation using the spring-ai-starter-model-anthropic dependency.
 * This configuration class serves as a placeholder for any additional AI-related
 * beans that may be needed beyond what Spring AI auto-configures.
 *
 * Required application properties when AI is enabled:
 * <pre>
 *   keza.ai.enabled=true
 *   spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}
 *   spring.ai.anthropic.chat.options.model=claude-sonnet-4-20250514
 *   spring.ai.anthropic.chat.options.max-tokens=1024
 * </pre>
 *
 * When keza.ai.enabled=false (or not set), the StubAiChatService is used instead
 * and no external API keys are required.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "keza.ai.enabled", havingValue = "true")
public class AiConfig {

    public AiConfig() {
        log.info("AI features are ENABLED - Spring AI Anthropic auto-configuration will provide ChatModel");
    }
}
