package com.maiphuhai.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class LlmStreamService {
    private final WebClient ollama;
    private final ObjectMapper om = new ObjectMapper();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LlmStreamService.class);

    public LlmStreamService(@Qualifier("ollamaWebClient") WebClient ollama) {
        this.ollama = ollama;
    }

    public void streamToEmitter(String prompt, SseEmitter emitter) {
        streamToEmitter(prompt, emitter, () -> {}, err -> {});
    }

    public void streamToEmitter(String prompt, SseEmitter emitter, Runnable onComplete, Consumer<Throwable> onError) {
        AtomicBoolean completed = new AtomicBoolean(false);
        StringBuilder carry = new StringBuilder(4096);

        ollama.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_NDJSON)
                .bodyValue("""
                {"model":"llama3.1:8b","prompt":%s,"stream":true}
            """.formatted(escapeJson(prompt)))
                .exchangeToFlux(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) {
                        return resp.bodyToFlux(byte[].class);
                    } else {
                        return resp.bodyToMono(String.class)
                                .doOnNext(body -> log.error("Ollama non-2xx {} body={}", resp.statusCode(), body))
                                .flatMapMany(s -> reactor.core.publisher.Flux.error(
                                        new IllegalStateException("Ollama HTTP " + resp.statusCode())));
                    }
                })
                .map(b -> new String(b, StandardCharsets.UTF_8))
                .doOnSubscribe(s -> log.info("Ollama stream started"))
                .subscribe(
                        chunk -> {
                            carry.append(chunk);
                            int idx;
                            while ((idx = indexOfNewline(carry)) >= 0) {
                                String line = carry.substring(0, idx).trim();
                                carry.delete(0, idx + 1);
                                if (line.isEmpty()) continue;
                                try {
                                    JsonNode node = om.readTree(line);
                                    if (node.hasNonNull("response")) {
                                        String piece = node.get("response").asText();
                                        emitter.send(SseEmitter.event()
                                                .name("delta")
                                                .data(om.writeValueAsString(java.util.Map.of("text", piece))));
                                    }
                                    if (node.has("done") && node.get("done").asBoolean(false)) {
                                        // Kết thúc từ server
                                        completed.set(true);
                                    }
                                } catch (Exception ex) {
                                    log.warn("Drop bad NDJSON line: {}", line, ex);
                                }
                            }
                        },
                        err -> {
                            log.error("Ollama stream error", err);
                            safeSend(emitter, SseEmitter.event().name("error")
                                    .data("{\"message\":\"ollama_error\"}"));
                            safeCompleteWithError(emitter, err);
                            onError.accept(err);
                        },
                        () -> {
                            log.info("Ollama stream completed");
                            onComplete.run();
                            if (!completed.get()) {
                                safeSend(emitter, SseEmitter.event().name("done").data("{}"));
                                safeComplete(emitter);
                            }
                        }
                );
    }

    private static int indexOfNewline(StringBuilder sb) {
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (c == '\n') return i;
        }
        return -1;
    }

    private static void safeSend(SseEmitter emitter, SseEmitter.SseEventBuilder ev) {
        try { emitter.send(ev); } catch (Exception ignored) {}
    }
    private static void safeComplete(SseEmitter emitter) {
        try { emitter.complete(); } catch (Exception ignored) {}
    }
    private static void safeCompleteWithError(SseEmitter emitter, Throwable t) {
        try { emitter.completeWithError(t); } catch (Exception ignored) {}
    }

    private static String escapeJson(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n") + "\"";
    }
}
