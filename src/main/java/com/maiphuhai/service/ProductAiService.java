package com.maiphuhai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maiphuhai.DTO.AiResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class ProductAiService {

    private final WebClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String model;

    public ProductAiService(WebClient aiWebClient,
                            @Value("${ai.model}") String model) {
        this.client = aiWebClient;
        this.model = model;
    }

    public AiResponseDTO ask(String userQuestion) throws JsonProcessingException {
        String prompt = """
      You are a product assistant. Return ONLY JSON with this schema:
      {
        "answer": string,
        "products": [
          {"id": number|null, "name": string|null, "company": string|null,
           "specs": string|null, "image": string|null}
        ]
      }
      No text before/after. User: %s
      """.formatted(userQuestion);

        // 2) Gọi Ollama generate (non-stream) + format:"json"
        Map<String,Object> req = Map.of(
                "model", model,
                "prompt", prompt,
                "format", "json",
                "stream", false
        );

        String raw = client.post()
                .uri("/api/generate")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        String content = mapper.readTree(raw).path("response").asText();

        // 4) Nếu model có bọc ```json ... ``` thì bóc ra
        content = extractJson(content);

        // 5) Parse an toàn -> nếu hỏng thì trả fallback thay vì quăng Exception
        try {
            return mapper.readValue(content, AiResponseDTO.class);
        } catch (Exception ex) {
            // Log để debug và trả fallback thay vì văng lỗi
            System.err.println("[AI RAW CONTENT]\n" + content);
            ex.printStackTrace();
            return new AiResponseDTO(
                    "Xin lỗi, mình chưa hiểu yêu cầu. Bạn thử diễn đạt lại ngắn gọn hơn nhé.",
                    List.of()
            );
        }
    }

    private String extractJson(String s) {
        if (s == null) return "{}";
        int i = s.indexOf('{');
        int j = s.lastIndexOf('}');
        if (i >= 0 && j > i) return s.substring(i, j + 1).trim();
        return s.trim();
    }
}

