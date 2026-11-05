package com.authbaseapp.authapp.Config;

import com.authbaseapp.authapp.DTOs.ApiResponse;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    public RateLimitFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return CorsUtils.isPreFlightRequest(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Bucket bucket = buckets.computeIfAbsent(request.getRemoteAddr(), ip -> Bucket.builder()
                .addLimit(limit -> limit.capacity(100).refillGreedy(100, Duration.ofMinutes(1))).build());
        if (!bucket.tryConsume(1)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            mapper.writeValue(response.getWriter(), ApiResponse.error("Rate limit exceeded. Try again later."));
            return;
        }
        chain.doFilter(request, response);
    }
}
