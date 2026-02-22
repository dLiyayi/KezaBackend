package com.keza.ai.domain.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DocumentTextExtractor")
class DocumentTextExtractorTest {

    private DocumentTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new DocumentTextExtractor();
    }

    @Test
    @DisplayName("should extract text from a valid PDF")
    void shouldExtractTextFromPdf() throws Exception {
        byte[] pdfBytes = createTestPdf("Hello World - Test Document");
        InputStream inputStream = new ByteArrayInputStream(pdfBytes);

        String result = extractor.extractText(inputStream, "application/pdf", "test.pdf");

        assertThat(result).contains("Hello World");
        assertThat(result).contains("Test Document");
    }

    @Test
    @DisplayName("should return empty string for JPEG images")
    void shouldReturnEmptyStringForJpeg() {
        InputStream inputStream = new ByteArrayInputStream(new byte[]{1, 2, 3});

        String result = extractor.extractText(inputStream, "image/jpeg", "photo.jpg");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty string for PNG images")
    void shouldReturnEmptyStringForPng() {
        InputStream inputStream = new ByteArrayInputStream(new byte[]{1, 2, 3});

        String result = extractor.extractText(inputStream, "image/png", "photo.png");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty string for null input stream")
    void shouldReturnEmptyStringForNullInputStream() {
        String result = extractor.extractText(null, "application/pdf", "test.pdf");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty string for null content type")
    void shouldReturnEmptyStringForNullContentType() {
        InputStream inputStream = new ByteArrayInputStream(new byte[]{1, 2, 3});

        String result = extractor.extractText(inputStream, null, "test.pdf");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty string for empty input")
    void shouldReturnEmptyStringForEmptyInput() {
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);

        String result = extractor.extractText(inputStream, "application/pdf", "empty.pdf");

        // Empty bytes will cause a PDFBox error, which should be handled gracefully
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty string for invalid PDF (graceful error handling)")
    void shouldReturnEmptyStringForInvalidPdf() {
        // Random bytes that are not a valid PDF
        byte[] invalidPdf = "This is not a PDF file at all".getBytes();
        InputStream inputStream = new ByteArrayInputStream(invalidPdf);

        String result = extractor.extractText(inputStream, "application/pdf", "invalid.pdf");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty string for unsupported content type")
    void shouldReturnEmptyStringForUnsupportedContentType() {
        InputStream inputStream = new ByteArrayInputStream(new byte[]{1, 2, 3});

        String result = extractor.extractText(inputStream, "application/zip", "archive.zip");

        assertThat(result).isEmpty();
    }

    /**
     * Creates a simple test PDF in memory with the given text content.
     */
    private byte[] createTestPdf(String text) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText(text);
                contentStream.endText();
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
