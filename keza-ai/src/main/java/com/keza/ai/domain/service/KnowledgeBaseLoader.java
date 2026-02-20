package com.keza.ai.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "keza.ai.enabled", havingValue = "true")
@ConditionalOnBean(VectorStore.class)
public class KnowledgeBaseLoader {

    private final VectorStore vectorStore;

    public KnowledgeBaseLoader(@Autowired VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadKnowledgeBase() {
        log.info("Loading knowledge base documents into vector store...");

        try {
            List<Document> documents = List.of(
                    // Platform FAQ
                    new Document("Keza is an AI-powered equity crowdfunding platform for East Africa. It enables retail investors to participate in startup and SME equity offerings with as little as KES 1,000.",
                            Map.of("category", "faq", "topic", "about")),

                    new Document("To invest on Keza, you need to: 1) Create an account, 2) Complete KYC verification by uploading your national ID or passport, 3) Browse available campaigns, 4) Select an investment amount within the campaign limits, 5) Pay via M-Pesa, card, or bank transfer.",
                            Map.of("category", "faq", "topic", "how-to-invest")),

                    new Document("KYC (Know Your Customer) verification requires uploading a valid government-issued ID (National ID, Passport, or Alien ID) and proof of address. Documents are reviewed within 24-48 hours. You cannot invest until KYC is approved.",
                            Map.of("category", "faq", "topic", "kyc")),

                    new Document("After investing, there is a mandatory 48-hour cooling-off period during which you can cancel your investment for a full refund. After this period, cancellations are not possible.",
                            Map.of("category", "faq", "topic", "cooling-off")),

                    new Document("The secondary marketplace allows you to sell your shares to other investors after a 12-month holding period. Company consent is required, and a 2% seller fee applies to all marketplace transactions.",
                            Map.of("category", "faq", "topic", "marketplace")),

                    // Regulatory info
                    new Document("Equity crowdfunding in Kenya is regulated by the Capital Markets Authority (CMA). Individual investors are limited to investing a maximum of 10% of their annual income in a single campaign and 20% aggregate across all campaigns.",
                            Map.of("category", "regulatory", "topic", "cma-limits")),

                    new Document("Campaign issuers must pass a comprehensive due diligence review covering legal compliance, financial health, management team, market analysis, and competitive positioning before their campaigns go live on the platform.",
                            Map.of("category", "regulatory", "topic", "due-diligence")),

                    // Payment info
                    new Document("Keza supports three payment methods: M-Pesa (instant via STK Push), card payments (Visa/Mastercard via Flutterwave), and bank transfer (KCB escrow accounts). M-Pesa is the most popular payment method in East Africa.",
                            Map.of("category", "payments", "topic", "methods")),

                    new Document("All investment funds are held in escrow accounts managed by KCB Bank until the campaign reaches its target or deadline. If a campaign fails to reach its minimum target, all funds are automatically refunded to investors.",
                            Map.of("category", "payments", "topic", "escrow")),

                    // Risk info
                    new Document("Investing in startups and SMEs carries significant risk, including the potential loss of your entire investment. Past performance is not indicative of future results. Diversify your investments and only invest money you can afford to lose.",
                            Map.of("category", "risk", "topic", "general-risk"))
            );

            vectorStore.add(documents);
            log.info("Successfully loaded {} knowledge base documents into vector store", documents.size());
        } catch (Exception e) {
            log.error("Failed to load knowledge base: {}", e.getMessage(), e);
        }
    }
}
