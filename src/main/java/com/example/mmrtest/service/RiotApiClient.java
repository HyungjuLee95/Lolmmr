package com.example.mmrtest.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

@Component
public class RiotApiClient {

    @Value("${riot.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public RiotApiClient() {
        this.restTemplate = new RestTemplate();
    }

    public <T> T get(String url, Class<T> responseType) {
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        ResponseEntity<T> resp = restTemplate.exchange(URI.create(url), HttpMethod.GET, entity, responseType);
        return resp.getBody();
    }

    public <T> T get(String url, ParameterizedTypeReference<T> responseType) {
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
        ResponseEntity<T> resp = restTemplate.exchange(URI.create(url), HttpMethod.GET, entity, responseType);
        return resp.getBody();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", apiKey);
        headers.set("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36");
        headers.set("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.set("Accept-Charset", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.set("Origin", "https://developer.riotgames.com");
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}
