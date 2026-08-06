package com.gym.bdd.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ApiClient {

    private final TestRestTemplate rest;
    private final ObjectMapper mapper;

    public ApiClient(TestRestTemplate rest, ObjectMapper mapper) {
        this.rest = rest;
        this.mapper = mapper;
    }

    public ResponseEntity<String> post(String path, Object body) {
        return post(path, body, new HttpHeaders());
    }

    public ResponseEntity<String> post(String path, Object body, HttpHeaders headers) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(toJson(body), headers), String.class);
    }

    public JsonNode read(ResponseEntity<String> response) {
        try {
            return mapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Response body is not valid JSON: " + response.getBody(), e);
        }
    }

    private String toJson(Object body) {
        if (body instanceof String s) {
            return s;
        }
        try {
            return mapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize request body", e);
        }
    }
}
