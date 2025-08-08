package com.maiphuhai.controller;

import com.maiphuhai.service.ProductAiService;
import com.maiphuhai.DTO.AiResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RecommendController {
    private final ProductAiService aiService;

    @Autowired
    public RecommendController(ProductAiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/recommend")
    public String recommend(@RequestParam("q") String query, Model model) {
        AiResponseDTO res = aiService.ask(query);

        model.addAttribute("answer", res.answer());
        model.addAttribute("products", res.products());

        return "recommend";
    }
}
