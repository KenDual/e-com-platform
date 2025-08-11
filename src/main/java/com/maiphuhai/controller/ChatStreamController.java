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
            emitter.send(SseEmitter.event()
                    .name("ready")
                    .data("{\"ts\":" + System.currentTimeMillis() + "}"));
        } catch (Exception e) {
            emitter.completeWithError(e);
            return emitter;
        }

        // 2) retrieve
        List<AiProduct> products;
        try {
            SearchResponse resp = retriever.search(q, topK);
            products = (resp != null && resp.getProducts() != null) ? resp.getProducts() : List.of();
        } catch (Exception e) {
            // retriever lỗi -> xin lỗi ngắn + final rỗng
            try {
                emitter.send(SseEmitter.event().name("delta")
                        .data("{\"text\":\"Sorry the system is busy.\"}"));
                String finalJson = om.writeValueAsString(Map.of(
                        "products", List.of(),
                        "turnsLeft", 4,
                        "error", "retriever_down"
                ));
                emitter.send(SseEmitter.event().name("final").data(finalJson));
            } catch (Exception ignored) {
            }
            emitter.complete();
            return emitter;
        }

        // 3) build prompt (plain text)
        String prompt = buildPrompt(q, products);

        // 4) stream LLM deltas
        llm.streamToEmitter(prompt, emitter);

        // 5) gửi final (khi đã bắn xong delta một lúc — ở đây gửi luôn sau 50ms)
        new Thread(() -> {
            try {
                Thread.sleep(50); // đủ để bắt đầu dòng delta
                String finalJson = om.writeValueAsString(Map.of(
                        "products", products,
                        "turnsLeft", 4,
                        "error", null
                ));
                emitter.send(SseEmitter.event().name("final").data(finalJson));
            } catch (Exception ignored) {
            } finally {
                emitter.complete();
            }
        }).start();

        return emitter;
    }

    private String buildPrompt(String q, List<AiProduct> products) {
        StringBuilder sb = new StringBuilder();
        sb.append("Người dùng hỏi: ").append(q).append("\n\n");
        sb.append("Dưới đây là 5 sản phẩm liên quan (name | brand | price(VND) | weight(kg) | battery | specs ngắn):\n");
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
        sb.append("\nHãy tư vấn ngắn gọn, rõ ràng bằng tiếng Việt, plaintext (không HTML). ");
        sb.append("Ưu tiên nói lý do chọn, và nêu 2-3 lựa chọn phù hợp nhất.\n");
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