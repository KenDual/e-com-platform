package com.maiphuhai.controller;


import com.maiphuhai.model.User;
import com.maiphuhai.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/login")
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String showLoginForm() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        Optinal <User> userOptinal = userService.
    }
}
