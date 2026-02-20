package com.keza.ai.domain.service;

import com.keza.ai.domain.model.ChatMessage;
import com.keza.ai.domain.port.out.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "keza.ai.enabled", havingValue = "true")
public class AiChatService {

    private final ChatModel chatModel;
    private final ChatMessageRepository chatMessageRepository;
    private final VectorStore vectorStore;

    public AiChatService(ChatModel chatModel, ChatMessageRepository chatMessageRepository,
                         @Autowired(required = false) VectorStore vectorStore) {
        this.chatModel = chatModel;
        this.chatMessageRepository = chatMessageRepository;
        this.vectorStore = vectorStore;
    }

    private static final String SYSTEM_PROMPT_EN = """
            You are Keza AI, a helpful financial assistant for the Keza equity crowdfunding platform in East Africa.
            You help users understand investments, campaigns, and financial concepts.
            You provide guidance on using the platform, understanding risk, and making informed investment decisions.
            Always be professional, clear, and honest. If you don't know something, say so.
            Never provide specific financial advice - always recommend consulting a licensed financial advisor for personal decisions.
            Keep responses concise and relevant to the East African investment context.
            """;

    private static final String SYSTEM_PROMPT_SW = """
            Wewe ni Keza AI, msaidizi wa kifedha wa jukwaa la Keza la uwekezaji wa umma mashariki mwa Afrika.
            Unasaidia watumiaji kuelewa uwekezaji, kampeni, na dhana za kifedha.
            Unatoa mwongozo wa kutumia jukwaa, kuelewa hatari, na kufanya maamuzi sahihi ya uwekezaji.
            Kuwa mtaalamu, wazi, na mkweli kila wakati. Ikiwa hujui kitu, sema hivyo.
            Usitoe ushauri maalum wa kifedha - pendekeza kila wakati kushauriana na mshauri wa kifedha aliye na leseni.
            """;

    private static final String SYSTEM_PROMPT_FR = """
            Vous etes Keza AI, un assistant financier utile pour la plateforme de financement participatif Keza en Afrique de l'Est.
            Vous aidez les utilisateurs a comprendre les investissements, les campagnes et les concepts financiers.
            Vous fournissez des conseils sur l'utilisation de la plateforme, la comprehension des risques et la prise de decisions d'investissement eclairees.
            Soyez toujours professionnel, clair et honnete. Si vous ne savez pas quelque chose, dites-le.
            Ne fournissez jamais de conseils financiers specifiques - recommandez toujours de consulter un conseiller financier agree.
            """;

    public String chat(UUID sessionId, String message, String language) {
        log.debug("Processing AI chat for session {} with language {}", sessionId, language);

        String systemPrompt = resolveSystemPrompt(language);

        // Retrieve recent conversation history for context
        List<ChatMessage> recentMessages = chatMessageRepository.findTop20BySessionIdOrderByCreatedAtDesc(sessionId);

        String conversationContext = recentMessages.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(msg -> msg.getRole() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));

        // RAG: retrieve relevant documents from vector store
        String ragContext = retrieveRelevantContext(message);

        // Build the full prompt with RAG context
        StringBuilder fullPrompt = new StringBuilder();

        if (!ragContext.isEmpty()) {
            fullPrompt.append("Relevant knowledge base information:\n")
                    .append(ragContext)
                    .append("\n\n");
        }

        if (!conversationContext.isEmpty()) {
            fullPrompt.append("Previous conversation:\n")
                    .append(conversationContext)
                    .append("\n\n");
        }

        fullPrompt.append("User: ").append(message);

        try {
            ChatClient chatClient = ChatClient.builder(chatModel).build();

            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(fullPrompt.toString())
                    .call()
                    .content();

            log.debug("AI response generated for session {}", sessionId);
            return response;
        } catch (Exception e) {
            log.error("Error calling AI model for session {}: {}", sessionId, e.getMessage(), e);
            return getErrorResponse(language);
        }
    }

    private String retrieveRelevantContext(String query) {
        if (vectorStore == null) {
            log.debug("VectorStore not available, skipping RAG retrieval");
            return "";
        }

        try {
            List<Document> documents = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(5)
                            .similarityThreshold(0.7)
                            .build());

            if (documents.isEmpty()) {
                return "";
            }

            return documents.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n---\n"));
        } catch (Exception e) {
            log.warn("Error during RAG retrieval: {}", e.getMessage());
            return "";
        }
    }

    private String resolveSystemPrompt(String language) {
        return switch (language.toLowerCase()) {
            case "sw" -> SYSTEM_PROMPT_SW;
            case "fr" -> SYSTEM_PROMPT_FR;
            default -> SYSTEM_PROMPT_EN;
        };
    }

    private String getErrorResponse(String language) {
        return switch (language.toLowerCase()) {
            case "sw" -> "Samahani, kuna tatizo la muda. Tafadhali jaribu tena baadaye.";
            case "fr" -> "Desole, il y a un probleme temporaire. Veuillez reessayer plus tard.";
            default -> "Sorry, there was a temporary issue processing your request. Please try again later.";
        };
    }
}
