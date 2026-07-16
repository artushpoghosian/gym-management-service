package com.gym.client;

import com.gym.security.JwtService;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeignAuthInterceptor implements RequestInterceptor {

    private static final String TRANSACTION_ID_HEADER = "X-Transaction-Id";
    private static final String TRANSACTION_ID_KEY = "transactionId";

    private final JwtService jwtService;

    @Override
    public void apply(RequestTemplate template) {
        template.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateServiceToken());

        String transactionId = MDC.get(TRANSACTION_ID_KEY);
        if (transactionId != null) {
            template.header(TRANSACTION_ID_HEADER, transactionId);
        }
    }
}
