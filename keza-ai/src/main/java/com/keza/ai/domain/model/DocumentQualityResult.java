package com.keza.ai.domain.model;

import java.util.List;

/**
 * Result of a document quality assessment performed before AI validation.
 *
 * @param qualityScore        quality score between 0.0 and 1.0
 * @param issues              list of quality issues detected
 * @param passesMinimumQuality whether the document meets minimum quality standards
 */
public record DocumentQualityResult(
        double qualityScore,
        List<String> issues,
        boolean passesMinimumQuality
) {}
