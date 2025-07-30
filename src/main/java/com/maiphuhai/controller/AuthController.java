package com.maiphuhai.controller;

import com.maiphuhai.model.User;
import com.maiphuhai.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/login")
public class AuthController {

    @Autowired
    private UserService userService;

    /* ----------- HIỂN THỊ FORM ----------- */
    @GetMapping
    public String showLoginForm(Model model) {
        return "auth/login";          // /WEB-INF/views/auth/login.jsp | .html
    }

    /* ----------- XỬ LÝ SUBMIT ----------- */
    @PostMapping
    public String doLogin(@RequestParam("identifier") String identifier, // username *hoặc* email
                          @RequestParam("password") String password,
                          HttpSession session,
                          Model model) {

        User user = userService.authenticate(identifier, password);

        if (user == null) {
            model.addAttribute("error", "Sai thông tin đăng nhập!");
            model.addAttribute("identifier", identifier);
            return "auth/login";
        }

        /* Lưu user vào session */
        session.setAttribute("currentUser", user);

        /* Điều hướng: admin → /admin, customer → / */
        return (user.getRoleId() == 1) ? "redirect:/admin" : "redirect:/";
    }

    /* ----------- LOGOUT THỦ CÔNG ----------- */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }
}
