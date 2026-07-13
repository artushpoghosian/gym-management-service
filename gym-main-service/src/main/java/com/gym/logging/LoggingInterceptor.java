package com.gym.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.UUID;

@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String TRANSACTION_ID = "transactionId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String txId = UUID.randomUUID().toString();
        MDC.put(TRANSACTION_ID, txId);
        log.info("[TX-START] {} {} transactionId={}", request.getMethod(), request.getRequestURI(), txId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex != null) {
            log.error("[TX-END] status={} transactionId={} error={}",
                    response.getStatus(), MDC.get(TRANSACTION_ID), ex.getMessage());
        } else {
            log.info("[TX-END] status={} transactionId={}",
                    response.getStatus(), MDC.get(TRANSACTION_ID));
        }
        MDC.remove(TRANSACTION_ID);
    }
}
