package com.maiphuhai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import java.time.Duration;

@Configuration
public class AiClientConfig {

    /**
     * Tạo WebClient với:
     *  • baseUrl lấy từ file properties
     *  • timeout (kết nối + đọc) cũng đọc từ properties
     *  • tăng mặc định buffersize để nhận JSON lớn (nếu cần)
     */
    @Bean
    public WebClient aiWebClient(
            @Value("${ai.base-url}") String baseUrl,
            @Value("${ai.timeout:60000}") long timeoutMs) {

        HttpClient http = HttpClient.create()
                .responseTimeout(Duration.ofMillis(timeoutMs));

        return WebClient.builder()
                .baseUrl(baseUrl) // http://localhost:11434
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(http))
                .build();
    }
}
