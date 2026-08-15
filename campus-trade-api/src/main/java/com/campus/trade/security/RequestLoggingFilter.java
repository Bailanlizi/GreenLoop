package com.campus.trade.security;

import com.campus.trade.common.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final static Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final int MAX_TRACE_ID_LENGTH = 64;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String traceId = resolveTraceId(request.getHeader(TRACE_ID_HEADER));
        long startTime = System.currentTimeMillis();
        TraceContext.setTraceId(traceId);
        MDC.put("traceId", traceId);
        request.setAttribute("traceId", traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info("api_request traceId={} method={} uri={} status={} durationMs={}",
                    traceId, request.getMethod(), request.getRequestURI(), response.getStatus(),
                    System.currentTimeMillis() - startTime);
            MDC.remove("traceId");
            TraceContext.clear();
        }
    }

    private String resolveTraceId(String requestTraceId) {
        if (StringUtils.hasText(requestTraceId) && requestTraceId.length() <= MAX_TRACE_ID_LENGTH
                && requestTraceId.matches("[A-Za-z0-9_-]+")) {
            return requestTraceId;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
