package com.wifisense.analysis;

import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;

@Component
public class HttpAiAnalysisClient implements AiAnalysisClient {

    private static final String DETECT_PATH = "/api/v1/anomalies/detect";

    private final RestClient restClient;

    public HttpAiAnalysisClient(RestClient.Builder builder, AiServiceProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.timeout());

        this.restClient = builder
                .baseUrl(properties.url())
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> {
                    if (StringUtils.hasText(properties.apiKey())) {
                        headers.set("X-API-Key", properties.apiKey());
                    }
                })
                .build();
    }

    @Override
    public AiAnalysisResponse detectAnomalies(AiAnalysisRequest request) {
        try {
            return restClient.post()
                    .uri(DETECT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiAnalysisResponse.class);
        } catch (RestClientException e) {
            throw new AiServiceUnavailableException("AI service request failed: " + e.getMessage(), e);
        }
    }
}
