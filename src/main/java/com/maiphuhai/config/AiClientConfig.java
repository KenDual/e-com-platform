package com.maiphuhai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
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
            @Value("${ai.timeout:5000}") long timeoutMs) {

        HttpClient nettyClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(timeoutMs))
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) timeoutMs);

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new
                        org.springframework.http.client.reactive.ReactorClientHttpConnector(nettyClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs()
                                .maxInMemorySize(4 * 1024 * 1024)) // 4 MB
                        .build())
                .build();
    }
}
