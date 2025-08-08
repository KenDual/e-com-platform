package com.maiphuhai.service;

import com.maiphuhai.DTO.AiResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Service
public class ProductAiService {
    private final WebClient client;
    private final Duration timeout;

    /**
     * @param aiWebClient  bean được tạo ở AiClientConfig
     * @param timeoutMs    lấy từ properties (mặc định 5 000 ms)
     */
    @Autowired
    public ProductAiService(WebClient aiWebClient,
                            @Value("${ai.timeout:5000}") long timeoutMs) {
        this.client  = aiWebClient;
        this.timeout = Duration.ofMillis(timeoutMs);

        //Log for AI chat
        System.out.println("[AI] baseUrl=" + aiWebClient + " timeout=" + timeoutMs);
    }

    /**
     * Gửi query đến /chat và chờ phản hồi AiResponseDTO.
     * Nếu AI-service trả lỗi 4xx/5xx sẽ ném RuntimeException cho Controller xử lý.
     */
    public AiResponseDTO ask(String query) {
        try {
            return client.post()
                    .uri("/chat")
                    .bodyValue(Map.of("query", query))
                    .retrieve()
                    .bodyToMono(AiResponseDTO.class)
                    .timeout(timeout)           // ngắt nếu server “im lặng”
                    .block();                   // biến Reactive → sync
        } catch (WebClientResponseException ex) {
            // log chi tiết và quăng ra 1 exception “gọn” hơn
            throw new IllegalStateException(
                    "AI service error " + ex.getRawStatusCode() + ": " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot connect to AI service: " + ex.getMessage(), ex);
        }

    }
}
