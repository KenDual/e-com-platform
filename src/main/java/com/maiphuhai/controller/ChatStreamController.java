package com.maiphuhai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import com.maiphuhai.DTO.AiProduct;
import com.maiphuhai.DTO.SearchResponse;
import com.maiphuhai.service.ai.LlmStreamService;
import com.maiphuhai.service.ai.RetrieverClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class ChatStreamController {

    private final RetrieverClient retriever;
    private final LlmStreamService llm;
    private final ObjectMapper om = new ObjectMapper();

    public ChatStreamController(RetrieverClient retriever, LlmStreamService llm) {
        this.retriever = retriever;
        this.llm = llm;
    }

    @GetMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam("q") String q,
                             @RequestParam(value = "top_k", defaultValue = "5") int topK) {

        SseEmitter emitter = new SseEmitter(0L);

        // 1) ready
        try {
            emitter.send(SseEmitter.event().name("ready")
                    .data("{\"ts\":" + System.currentTimeMillis() + "}"));
        } catch (Exception e) {
            emitter.completeWithError(e);
            return emitter;
        }

        // 2) retrieve
        final java.util.List<AiProduct> products;
        try {
            SearchResponse resp = retriever.search(q, topK);
            products = (resp != null && resp.getProducts() != null) ? resp.getProducts() : java.util.List.of();
        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event().name("delta")
                        .data("{\"text\":\"Sorry, the system is busy...\"}"));
                String finalJson = om.writeValueAsString(java.util.Map.of(
                        "products", java.util.List.of(),
                        "turnsLeft", 4,
                        "error", "retriever_down"
                ));
                emitter.send(SseEmitter.event().name("final").data(finalJson));
                emitter.send(SseEmitter.event().name("done").data("{}"));
            } catch (Exception ignored) {}
            emitter.complete();
            return emitter;
        }

        // 3) prompt
        String prompt = buildPrompt(q, products);

        // 4) stream LLM deltas -> khi xong: gửi products rồi done
        try {
            llm.streamToEmitter(
                    prompt,
                    emitter,
                    () -> { // onComplete
                        try {
                            String json = om.writeValueAsString(products);
                            emitter.send(SseEmitter.event().name("products").data(json));
                            emitter.send(SseEmitter.event().name("done").data("{}"));
                        } catch (Exception ignored) {
                        } finally {
                            emitter.complete();
                        }
                    },
                    (Throwable err) -> { // onError
                        try {
                            emitter.send(SseEmitter.event().name("delta")
                                    .data("{\"text\":\"Sorry, is seems AI got some trouble while answering.\"}"));
                            emitter.send(SseEmitter.event().name("done").data("{}"));
                        } catch (Exception ignored) {}
                        emitter.complete();
                    }
            );
        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event().name("delta")
                        .data("{\"text\":\"Sorry, AI can't start up.\"}"));
                emitter.send(SseEmitter.event().name("done").data("{}"));
            } catch (Exception ignored) {}
            emitter.complete();
        }

        return emitter;
    }

    private String buildPrompt(String q, List<AiProduct> products) {
        StringBuilder sb = new StringBuilder();
        sb.append("User ask: ").append(q).append("\n\n");
        sb.append("Here are up to 5 related products (name | brand | price(VND) | weight(kg) | battery | brief specs):\n");
        for (int i = 0; i < products.size(); i++) {
            AiProduct p = products.get(i);
            sb.append(i + 1).append(". ")
                    .append(s(p.getName())).append(" | ")
                    .append(s(p.getBrand())).append(" | ")
                    .append(p.getPrice() == null ? "?" : String.valueOf(p.getPrice().longValue())).append(" | ")
                    .append(p.getWeight() == null ? "?" : p.getWeight()).append(" | ")
                    .append(s(p.getBattery())).append(" | ")
                    .append(trimSpecs(p.getSpecs(), 180))
                    .append("\n");
        }
        sb.append("You are a laptop shopping assistant. ");
        sb.append("\nAnswer in ENGLISH ONLY. Plain text (no Markdown/HTML). ");
        sb.append("Prioritize your choice by stating the reason, and list 2-3 most suitable options.\n");
        return sb.toString();
    }

    private static String s(String x) {
        return x == null ? "" : x;
    }

    private static String trimSpecs(String specs, int max) {
        if (specs == null) return "";
        return specs.length() <= max ? specs : specs.substring(0, max - 1) + "…";
    }
}