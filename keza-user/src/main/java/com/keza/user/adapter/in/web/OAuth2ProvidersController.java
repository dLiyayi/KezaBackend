package com.keza.user.adapter.in.web;

import com.keza.common.dto.ApiResponse;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Public discovery endpoint so the frontend knows which social OAuth2 providers are available.
 */
@RestController
@RequestMapping("/api/v1/auth/social")
@ConditionalOnProperty(name = "keza.oauth2.enabled", havingValue = "true")
public class OAuth2ProvidersController {

    @Value("${keza.oauth2.google.client-id:}")
    private String googleClientId;

    @Value("${keza.oauth2.facebook.client-id:}")
    private String facebookClientId;

    @Value("${keza.oauth2.apple.client-id:}")
    private String appleClientId;

    @Value("${keza.oauth2.kcb.client-id:}")
    private String kcbClientId;

    @GetMapping("/providers")
    public ResponseEntity<ApiResponse<List<ProviderInfo>>> getProviders() {
        List<ProviderInfo> providers = new ArrayList<>();

        if (!googleClientId.isBlank()) {
            providers.add(ProviderInfo.builder()
                    .name("google")
                    .displayName("Google")
                    .authorizationUrl("/oauth2/authorization/google")
                    .icon("google")
                    .build());
        }
        if (!facebookClientId.isBlank()) {
            providers.add(ProviderInfo.builder()
                    .name("facebook")
                    .displayName("Facebook")
                    .authorizationUrl("/oauth2/authorization/facebook")
                    .icon("facebook")
                    .build());
        }
        if (!appleClientId.isBlank()) {
            providers.add(ProviderInfo.builder()
                    .name("apple")
                    .displayName("Apple")
                    .authorizationUrl("/oauth2/authorization/apple")
                    .icon("apple")
                    .build());
        }
        if (!kcbClientId.isBlank()) {
            providers.add(ProviderInfo.builder()
                    .name("kcb")
                    .displayName("Kenya Commercial Bank")
                    .authorizationUrl("/oauth2/authorization/kcb")
                    .icon("kcb")
                    .build());
        }

        return ResponseEntity.ok(ApiResponse.success(providers));
    }

    @Data
    @Builder
    public static class ProviderInfo {
        private String name;
        private String displayName;
        private String authorizationUrl;
        private String icon;
    }
}
