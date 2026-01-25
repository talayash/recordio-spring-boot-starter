package com.lognet.recordio.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * Filter that wraps HTTP requests and responses to enable body content caching.
 * <p>
 * This allows the RecordIOAspect to read request/response bodies after they have
 * been consumed by the controller.
 */
public class RecordIOFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Wrap request if not already wrapped
        HttpServletRequest wrappedRequest = request;
        if (!(request instanceof ContentCachingRequestWrapper)) {
            wrappedRequest = new ContentCachingRequestWrapper(request);
        }

        // Wrap response if not already wrapped
        HttpServletResponse wrappedResponse = response;
        if (!(response instanceof ContentCachingResponseWrapper)) {
            wrappedResponse = new ContentCachingResponseWrapper(response);
        }

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            // Copy response body to the actual response
            if (wrappedResponse instanceof ContentCachingResponseWrapper wrapper) {
                wrapper.copyBodyToResponse();
            }
        }
    }
}
