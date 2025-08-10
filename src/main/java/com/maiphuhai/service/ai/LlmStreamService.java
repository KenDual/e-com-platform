package com.maiphuhai.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@Service
public class LlmStreamService {
    private final WebClient ollama;
    private final ObjectMapper om = new ObjectMapper();

    public LlmStreamService(@Qualifier("ollamaWebClient") WebClient ollama) {
        this.ollama = ollama;
    }

    public void streamToEmitter(String prompt, SseEmitter emitter) {
        AtomicBoolean done = new AtomicBoolean(false);

        Flux<String> flux = ollama.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue("""
                          {"model":"llama3.1:8b","prompt":%s,"stream":true}
                        """.formatted(escapeJson(prompt)))
                .retrieve()
                .bodyToFlux(byte[].class)
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .timeout(Duration.ofMinutes(2));

        flux.subscribe(
                chunk -> {
                    for (String line : chunk.split("\\r?\\n")) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty()) continue;
                        try {
                            JsonNode node = om.readTree(trimmed);
                            if (node.has("response")) {
                                String delta = node.get("response").asText("");
                                if (!delta.isEmpty()) {
                                    SseEmitter.event()
                                            .name("delta")
                                            .data("{\"text\":" + escapeJson(delta) + "}")
                                            .id(null);
                                    emitter.send(SseEmitter.event().name("delta")
                                            .data("{\"text\":" + escapeJson(delta) + "}"));
                                }
                            }
                            if (node.has("done") && node.get("done").asBoolean(false)) {
                                done.set(true);
                            }
                        } catch (Exception ignore) {
                            // dòng không phải JSON hợp lệ -> bỏ qua
                        }
                    }
                },
                err -> {
                    try {
                        emitter.send(SseEmitter.event().name("error")
                                .data("{\"message\":\"ollama_error\"}"));
                    } catch (Exception ignored) {
                    }
                    emitter.completeWithError(err);
                },
                () -> {
                    if (!done.get()) {
                        // kết thúc không có "done":true vẫn đóng
                    }
                    // không gửi final ở đây; controller sẽ gửi để kèm products
                    // chỉ complete khi controller bảo
                }
        );
    }

    private static String escapeJson(String s) {
        return "\"" + s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n") + "\"";
    }
}