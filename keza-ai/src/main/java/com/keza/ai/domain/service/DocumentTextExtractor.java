package com.keza.ai.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Service responsible for extracting text content from uploaded KYC documents.
 * <p>
 * Supports PDF text extraction via Apache PDFBox. Image-based documents (JPEG, PNG)
 * return an empty string as a placeholder for future OCR integration (e.g., Google Cloud Vision).
 */
@Slf4j
@Service
public class DocumentTextExtractor {

    /**
     * Extracts text from a document based on its content type.
     *
     * @param inputStream the document's input stream
     * @param contentType the MIME content type of the document
     * @param fileName    the original file name (used for logging)
     * @return extracted text, or empty string if extraction is not supported or fails
     */
    public String extractText(InputStream inputStream, String contentType, String fileName) {
        if (inputStream == null) {
            log.warn("Cannot extract text: input stream is null for file '{}'", fileName);
            return "";
        }

        if (contentType == null) {
            log.warn("Cannot extract text: content type is null for file '{}'", fileName);
            return "";
        }

        return switch (contentType.toLowerCase()) {
            case "application/pdf" -> extractFromPdf(inputStream, fileName);
            case "image/jpeg", "image/jpg", "image/png" -> {
                log.info("OCR is not configured. Skipping text extraction for image file '{}' (type: {}). " +
                        "Consider integrating Google Cloud Vision for image-based document processing.", fileName, contentType);
                yield "";
            }
            default -> {
                log.warn("Unsupported content type '{}' for text extraction from file '{}'", contentType, fileName);
                yield "";
            }
        };
    }

    private String extractFromPdf(InputStream inputStream, String fileName) {
        try {
            byte[] pdfBytes = inputStream.readAllBytes();
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                log.info("Successfully extracted {} characters from PDF '{}'", text.length(), fileName);
                return text;
            }
        } catch (Exception e) {
            log.error("Failed to extract text from PDF '{}': {}", fileName, e.getMessage(), e);
            return "";
        }
    }
}
