package com.keza.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "keza.ai.enabled", havingValue = "true")
public class AiConfig {

    public AiConfig() {
        log.info("AI features are ENABLED - Spring AI Anthropic auto-configuration will provide ChatModel");
    }

    @Bean
    @ConditionalOnBean(EmbeddingModel.class)
    public PgVectorStore vectorStore(EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate) {
        log.info("Configuring PgVectorStore for RAG pipeline");
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .schemaName("public")
                .dimensions(1536)
                .build();
    }
}
