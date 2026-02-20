package com.keza.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestLoggingFilter")
class RequestLoggingFilterTest {

    private RequestLoggingFilter filter;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setMethod("GET");
        request.setRequestURI("/api/v1/campaigns");
        request.setRemoteAddr("127.0.0.1");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    @DisplayName("Correlation ID handling")
    class CorrelationId {

        @Test
        @DisplayName("should generate a new correlation ID when header is not present")
        void shouldGenerateCorrelationIdWhenMissing() throws ServletException, IOException {
            doAnswer(invocation -> {
                assertThat(MDC.get("correlationId")).isNotNull().isNotEmpty();
                return null;
            }).when(filterChain).doFilter(any(), any());

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getHeader("X-Correlation-Id")).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("should use existing correlation ID from request header")
        void shouldUseExistingCorrelationId() throws ServletException, IOException {
            String existingId = "test-correlation-123";
            request.addHeader("X-Correlation-Id", existingId);

            doAnswer(invocation -> {
                assertThat(MDC.get("correlationId")).isEqualTo(existingId);
                return null;
            }).when(filterChain).doFilter(any(), any());

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getHeader("X-Correlation-Id")).isEqualTo(existingId);
        }

        @Test
        @DisplayName("should generate new correlation ID when header is blank")
        void shouldGenerateNewIdWhenHeaderBlank() throws ServletException, IOException {
            request.addHeader("X-Correlation-Id", "   ");

            doAnswer(invocation -> {
                assertThat(MDC.get("correlationId")).isNotBlank();
                return null;
            }).when(filterChain).doFilter(any(), any());

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getHeader("X-Correlation-Id")).isNotBlank();
            assertThat(response.getHeader("X-Correlation-Id")).isNotEqualTo("   ");
        }

        @Test
        @DisplayName("should set correlation ID on response header")
        void shouldSetCorrelationIdOnResponse() throws ServletException, IOException {
            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getHeader("X-Correlation-Id")).isNotNull().isNotEmpty();
        }
    }

    @Nested
    @DisplayName("MDC cleanup")
    class MdcCleanup {

        @Test
        @DisplayName("should clear MDC correlationId after filter completes")
        void shouldClearMdcAfterCompletion() throws ServletException, IOException {
            filter.doFilterInternal(request, response, filterChain);

            assertThat(MDC.get("correlationId")).isNull();
        }

        @Test
        @DisplayName("should clear MDC correlationId even when filter chain throws exception")
        void shouldClearMdcOnException() throws ServletException, IOException {
            doThrow(new ServletException("Something went wrong"))
                    .when(filterChain).doFilter(any(), any());

            assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                    .isInstanceOf(ServletException.class);

            assertThat(MDC.get("correlationId")).isNull();
        }
    }

    @Nested
    @DisplayName("Filter chain invocation")
    class FilterChainInvocation {

        @Test
        @DisplayName("should invoke the filter chain")
        void shouldInvokeFilterChain() throws ServletException, IOException {
            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should propagate exceptions from filter chain")
        void shouldPropagateExceptions() throws ServletException, IOException {
            doThrow(new IOException("Connection reset"))
                    .when(filterChain).doFilter(any(), any());

            assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                    .isInstanceOf(IOException.class)
                    .hasMessage("Connection reset");
        }
    }

    @Nested
    @DisplayName("Correlation ID uniqueness")
    class CorrelationIdUniqueness {

        @Test
        @DisplayName("should generate unique correlation IDs across requests")
        void shouldGenerateUniqueIds() throws ServletException, IOException {
            MockHttpServletResponse response1 = new MockHttpServletResponse();
            MockHttpServletResponse response2 = new MockHttpServletResponse();

            filter.doFilterInternal(request, response1, filterChain);
            filter.doFilterInternal(request, response2, filterChain);

            String id1 = response1.getHeader("X-Correlation-Id");
            String id2 = response2.getHeader("X-Correlation-Id");

            assertThat(id1).isNotEqualTo(id2);
        }
    }
}
