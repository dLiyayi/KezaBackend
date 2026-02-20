package com.keza.user.adapter.in.messaging;

import com.keza.infrastructure.config.RabbitMQConfig;
import com.keza.user.domain.port.in.DocumentProcessingPort;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KycDocumentListener {

    private final DocumentProcessingPort documentProcessingPort;

    @RabbitListener(queues = RabbitMQConfig.KYC_PROCESSING_QUEUE, ackMode = "MANUAL")
    public void handleKycDocumentProcessing(UUID documentId, Channel channel,
                                            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("Received KYC document processing request: documentId={}", documentId);

        try {
            documentProcessingPort.processDocument(documentId);
            channel.basicAck(deliveryTag, false);
            log.info("KYC document processing completed and acknowledged: documentId={}", documentId);
        } catch (Exception e) {
            log.error("Failed to process KYC document: documentId={}", documentId, e);
            try {
                // Reject the message without requeue (it will go to DLQ)
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("Failed to nack KYC document message: documentId={}", documentId, ioException);
            }
        }
    }
}
