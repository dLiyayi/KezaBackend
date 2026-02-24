package com.keza.ai.application.usecase;

import com.keza.ai.application.dto.DocumentValidationRequest;
import com.keza.ai.application.dto.DocumentValidationResponse;

public abstract class DocumentValidationUseCaseBase {

    public abstract DocumentValidationResponse validateDocument(DocumentValidationRequest request);
}
