package com.MyAnimaLog.Pets.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.*;

class InternalApiKeyFilterTest {

    private static final String KEY = "clave-interna-de-prueba";

    private final RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void constructor_shouldFailFast_whenKeyIsBlank() {
        assertThatThrownBy(() -> new InternalApiKeyFilter("   ", entryPoint))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INTERNAL_API_KEY");
    }

    @Test
    void constructor_shouldFailFast_whenKeyIsNull() {
        assertThatThrownBy(() -> new InternalApiKeyFilter(null, entryPoint))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INTERNAL_API_KEY");
    }

    @Test
    void shouldAuthenticate_whenConfiguredKeyHasSurroundingWhitespace() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter(" " + KEY + "\n", entryPoint);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(internalRequest(KEY), response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .containsExactly("INTERNAL_SERVICE");
    }

    @Test
    void shouldReject_whenKeyDoesNotMatch() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new InternalApiKeyFilter(KEY, entryPoint).doFilter(internalRequest("clave-falsa"), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"error\"");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void shouldReject_whenHeaderIsMissing() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new InternalApiKeyFilter(KEY, entryPoint).doFilter(internalRequest(null), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void shouldReject_whenHeaderIsEmptyAndWouldMatchABlankKey() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new InternalApiKeyFilter(KEY, entryPoint).doFilter(internalRequest(""), response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void shouldIgnoreNonInternalRoutes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/pets/abc");
        request.setRequestURI("/pets/abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new InternalApiKeyFilter(KEY, entryPoint).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    private MockHttpServletRequest internalRequest(String headerValue) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/pets/x/owner/y");
        request.setRequestURI("/internal/pets/x/owner/y");
        if (headerValue != null) {
            request.addHeader("X-Internal-Api-Key", headerValue);
        }
        return request;
    }
}
