package com.gym.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.UUID;

@Slf4j
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String TRANSACTION_ID = "transactionId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String txId = UUID.randomUUID().toString();
        MDC.put(TRANSACTION_ID, txId);
        log.info("Endpoint: {} | Method: {} | TransactionId: {}", request.getRequestURI(), request.getMethod(), txId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex != null) {
            log.error("Request failed with status: {} | Error: {}", response.getStatus(), ex.getMessage());
        } else {
            log.info("Request completed with status: {}", response.getStatus());
        }
        MDC.remove(TRANSACTION_ID);
    }
}