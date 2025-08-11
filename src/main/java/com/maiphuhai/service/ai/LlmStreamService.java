package com.maiphuhai.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class LlmStreamService {
    private final WebClient ollama;
    private final ObjectMapper om = new ObjectMapper();
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(LlmStreamService.class);

    public LlmStreamService(@Qualifier("ollamaWebClient") WebClient ollama) {
        this.ollama = ollama;
    }

    public void streamToEmitter(String prompt, SseEmitter emitter) {
        AtomicBoolean done = new AtomicBoolean(false);
        StringBuilder buf = new StringBuilder(4096);

        ollama.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_NDJSON) // <- quan trọng
                .bodyValue("""
                            {"model":"llama3.1:8b","prompt":%s,"stream":true}
                        """.formatted(escapeJson(prompt)))
                .exchangeToFlux(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) {
                        return resp.bodyToFlux(byte[].class);
                    } else {
                        // đọc body lỗi để log ra, dễ debug khi model không tồn tại
                        return resp.bodyToMono(String.class)
                                .doOnNext(body -> log.error("Ollama non-2xx {} body={}", resp.statusCode(), body))
                                .flatMapMany(s -> reactor.core.publisher.Flux.error(
                                        new IllegalStateException("Ollama HTTP " + resp.statusCode())
                                ));
                    }
                })
                .map(b -> new String(b, StandardCharsets.UTF_8))
                .doOnSubscribe(s -> log.info("Ollama stream started"))
                .subscribe(
                        /* onNext */ chunk -> { /* như cũ */ },
                        /* onError */ err -> {
                            log.error("Ollama stream error", err);
                            try {
                                emitter.send(SseEmitter.event().name("error")
                                        .data("{\"message\":\"ollama_error\"}"));
                            } catch (Exception ignore) {
                            }
                            emitter.completeWithError(err);
                        },
                        /* onComplete */ () -> {
                            log.info("Publisher completed");
                            if (!done.get()) {
                                try {
                                    emitter.send(SseEmitter.event().name("done").data("{}"));
                                } catch (Exception ignore) {
                                }
                                emitter.complete();
                            }
                        }
                );

    }

    private static String escapeJson(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n") + "\"";
    }
}
