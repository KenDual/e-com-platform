package com.maiphuhai.service.ai;

import com.maiphuhai.DTO.SearchResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class RetrieverClient {
    private final WebClient client;

    public RetrieverClient(@Qualifier("retrieverWebClient") WebClient client) {
        this.client = client;
    }

    public SearchResponse search(String q, int topK) {
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", q)
                        .queryParam("top_k", topK)
                        .build())
                .retrieve()
                .bodyToMono(SearchResponse.class)
                .block();
    }
}