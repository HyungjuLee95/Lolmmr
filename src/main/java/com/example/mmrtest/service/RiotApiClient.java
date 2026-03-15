package com.example.mmrtest.service;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
public class RiotApiClient {

    private static final Logger log = LoggerFactory.getLogger(RiotApiClient.class);

    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long DEFAULT_RETRY_SLEEP_MS = 1200L;

    @Value("${riot.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public RiotApiClient(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    public <T> T get(String url, Class<T> responseType) {
        return executeWithRetry(() -> {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<T> resp =
                    restTemplate.exchange(URI.create(url), HttpMethod.GET, entity, responseType);
            return resp.getBody();
        }, url);
    }

    public <T> T get(String url, ParameterizedTypeReference<T> responseType) {
        return executeWithRetry(() -> {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders());
            ResponseEntity<T> resp =
                    restTemplate.exchange(URI.create(url), HttpMethod.GET, entity, responseType);
            return resp.getBody();
        }, url);
    }

    private HttpHeaders buildHeaders() {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Riot API key is missing. Check RIOT_API_KEY environment variable.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", apiKey);
        headers.set(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36"
        );
        headers.set("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.set("Accept-Charset", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.set("Origin", "https://developer.riotgames.com");
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private <T> T executeWithRetry(RiotCall<T> call, String url) {
        int attempt = 1;

        while (true) {
            try {
                return call.execute();
            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();

                if (status == 429 && attempt < DEFAULT_MAX_ATTEMPTS) {
                    long sleepMs = resolveRetryDelayMs(e);
                    log.warn(
                            "Riot API rate limited. status=429 attempt={}/{} sleepMs={} url={}",
                            attempt,
                            DEFAULT_MAX_ATTEMPTS,
                            sleepMs,
                            shortenUrl(url)
                    );
                    sleepQuietly(sleepMs);
                    attempt++;
                    continue;
                }

                log.error(
                        "Riot API request failed. status={} attempt={}/{} url={} body={}",
                        status,
                        attempt,
                        DEFAULT_MAX_ATTEMPTS,
                        shortenUrl(url),
                        trimBody(e.getResponseBodyAsString())
                );
                throw e;
            } catch (ResourceAccessException e) {
                if (attempt < DEFAULT_MAX_ATTEMPTS) {
                    long sleepMs = DEFAULT_RETRY_SLEEP_MS;
                    log.warn(
                            "Riot API network timeout/access error. attempt={}/{} sleepMs={} url={} cause={}",
                            attempt,
                            DEFAULT_MAX_ATTEMPTS,
                            sleepMs,
                            shortenUrl(url),
                            e.getMessage()
                    );
                    sleepQuietly(sleepMs);
                    attempt++;
                    continue;
                }

                log.error(
                        "Riot API network access failed after retries. attempt={}/{} url={} cause={}",
                        attempt,
                        DEFAULT_MAX_ATTEMPTS,
                        shortenUrl(url),
                        e.getMessage()
                );
                throw e;
            }
        }
    }

    private long resolveRetryDelayMs(HttpStatusCodeException e) {
        String retryAfter = e.getResponseHeaders() != null
                ? e.getResponseHeaders().getFirst("Retry-After")
                : null;

        if (StringUtils.hasText(retryAfter)) {
            try {
                long seconds = Long.parseLong(retryAfter.trim());
                return Math.max(seconds * 1000L, DEFAULT_RETRY_SLEEP_MS);
            } catch (NumberFormatException ignored) {
                // fallback below
            }
        }

        return DEFAULT_RETRY_SLEEP_MS;
    }

    private void sleepQuietly(long sleepMs) {
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Riot API retry.", ie);
        }
    }

    private String shortenUrl(String url) {
        if (url == null) {
            return "";
        }
        return url.length() > 180 ? url.substring(0, 180) + "..." : url;
    }

    private String trimBody(String body) {
        if (body == null) {
            return "";
        }
        String trimmed = body.replaceAll("\\s+", " ").trim();
        return trimmed.length() > 300 ? trimmed.substring(0, 300) + "..." : trimmed;
    }

    @FunctionalInterface
    private interface RiotCall<T> {
        T execute();
    }
}