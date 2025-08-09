package com.maiphuhai.controller;

import com.maiphuhai.DTO.AiResponseDTO;
import com.maiphuhai.DTO.ChatProductVM;
import com.maiphuhai.DTO.ChatResponseVM;
import com.maiphuhai.DTO.ProductBrief;
import com.maiphuhai.repository.ProductRepository;
import com.maiphuhai.service.ProductAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ChatController {
    private final ProductAiService aiService;
    private final ProductRepository productRepository;

    @Autowired
    public ChatController(ProductAiService aiService, ProductRepository productRepository) {
        this.aiService = aiService;
        this.productRepository = productRepository;
    }

    // 1) Trang thử nghiệm chatbot
    @GetMapping("/chatbox")
    public String chatbox() {
        return "chatbox"; // /WEB-INF/views/chatbox.html
    }

    // 2) API JSON cho trang chatbox (GET để khỏi dính CSRF)
    @GetMapping(value = "/api/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ChatResponseVM chat(@RequestParam("q") String q) {
        try {
            AiResponseDTO res = aiService.ask(q);

            List<Integer> ids = res.products() == null ? List.of() :
                    res.products().stream()
                            .map(p -> p.id() == null ? null : p.id().intValue())
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());

            // ⬇️ Quan trọng: KHÔNG gọi repo nếu ids rỗng
            Map<Integer, ProductBrief> briefMap = ids.isEmpty()
                    ? Map.of()
                    : productRepository.findBriefByIds(ids);

            List<ChatProductVM> vms = res.products() == null ? List.of() :
                    res.products().stream().map(p -> {
                        ProductBrief b = (p.id() == null) ? null : briefMap.get(p.id().intValue());
                        Long priceFromDb = (b == null) ? null : b.priceVnd();
                        String img = (p.image() != null && !p.image().isBlank())
                                ? p.image()
                                : (b != null ? b.imageCover() : null);

                        return new ChatProductVM(
                                p.id(),
                                p.name(),
                                p.company(),
                                priceFromDb,
                                p.specs(),
                                img,
                                b != null ? b.description() : null
                        );
                    }).collect(Collectors.toList());

            return new ChatResponseVM(res.answer(), vms);
        } catch (Exception ex) {
            // Log thật rõ để lần sau tra nhanh stacktrace
            ex.printStackTrace();
            return new ChatResponseVM(
                    "Sorry,the system is busy. Please wait for a second ",
                    List.of()
            );
        }
    }
}
