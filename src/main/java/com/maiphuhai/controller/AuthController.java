package com.maiphuhai.controller;

import com.maiphuhai.DTO.PasswordResetToken;
import com.maiphuhai.DTO.UserRegistration;
import com.maiphuhai.model.User;
import com.maiphuhai.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    /* ----------- HIỂN THỊ FORM ----------- */
    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "page", required = false) String page,
                                Model model) {
        if (!model.containsAttribute("userRegistration"))
            model.addAttribute("userRegistration", new UserRegistration());

        model.addAttribute("page", page);
        return "auth/auth";
    }

    /* ----------- XỬ LÝ LOGIN ----------- */
    @PostMapping("/login")
    public String doLogin(@RequestParam String email,
                          @RequestParam String password,
                          HttpSession session,
                          RedirectAttributes ra) {

        User user = userService.authenticate(email, password);
        if (user == null) {
            ra.addFlashAttribute("error", "Sai thông tin đăng nhập!");
            ra.addAttribute("page", "login");
            return "redirect:/login";
        }
        session.setAttribute("currentUser", user);
        return (user.getRoleId()==1) ? "redirect:/admin" : "redirect:/";
    }

    /* ----------- Xử lý sign-up ----------- */
    @PostMapping("/signup")
    public String signup(@Validated @ModelAttribute("userRegistration") UserRegistration reg,
                         BindingResult br,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("org.springframework.validation.BindingResult.userRegistration", br);
            ra.addFlashAttribute("userRegistration", reg);
            ra.addAttribute("page", "signup");
            return "redirect:/login";
        }
        try {
            userService.register(reg.getUsername(), reg.getEmail(), reg.getPassword());
            ra.addFlashAttribute("signupSuccess", "Account created successfully!");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("signupError", ex.getMessage());
            ra.addFlashAttribute("userRegistration", reg);
            ra.addAttribute("page", "signup");
        }
        return "redirect:/login";
    }

    /* ========== FORGOT PASSWORD FLOW (simplified) ========== */
    private final Map<String, String> otpStore = new ConcurrentHashMap<>();
    private final Map<String, PasswordResetToken> resetStore = new ConcurrentHashMap<>();

    /* --- Gửi OTP --- */
    @PostMapping("/forgot-password")
    public String forgot(@RequestParam("email") String email,
                         RedirectAttributes ra) {
        User u;
        try {
            u = userService.findByUsernameOrEmail(email);
        } catch (Exception e) {
            u = null;
        }
        if (u == null) {
            ra.addFlashAttribute("otpError", "Email không tồn tại!");
            ra.addAttribute("page", "forgot");
            return "redirect:/login";
        }
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        otpStore.put(email, otp);
        ra.addFlashAttribute("email", email);
        return "redirect:/login?page=otp";
    }

    /* --- Xác thực OTP --- */
    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String email,
                            @RequestParam String otp1, @RequestParam String otp2,
                            @RequestParam String otp3, @RequestParam String otp4,
                            @RequestParam String otp5, @RequestParam String otp6,
                            RedirectAttributes ra) {
        String code = otp1 + otp2 + otp3 + otp4 + otp5 + otp6;
        if (!code.equals(otpStore.getOrDefault(email, ""))) {
            ra.addFlashAttribute("otpError", "Mã OTP sai!");
            ra.addAttribute("page", "otp");
            return "redirect:/login";
        }
        // sinh token reset
        String token = UUID.randomUUID().toString();
        PasswordResetToken prt = new PasswordResetToken();
        prt.setEmail(email);
        prt.setToken(token);
        prt.setExpiry(LocalDateTime.now().plusMinutes(15));
        resetStore.put(token, prt);
        ra.addFlashAttribute("resetToken", token);
        return "redirect:/login?page=reset";
    }

    /* --- Đặt mật khẩu mới --- */
    @PostMapping("/reset-password")
    public String resetPwd(@RequestParam String token,
                           @RequestParam String newPassword,
                           @RequestParam String confirmNewPassword,
                           RedirectAttributes ra){

        PasswordResetToken prt = resetStore.get(token);
        if(prt==null || prt.getExpiry().isBefore(LocalDateTime.now())){
            ra.addFlashAttribute("otpError","Token hết hạn!");
            ra.addAttribute("page","forgot");
            return "redirect:/login";
        }
        if(!newPassword.equals(confirmNewPassword)){
            ra.addFlashAttribute("otpError","Mật khẩu nhập lại không khớp!");
            ra.addFlashAttribute("resetToken", token);
            ra.addAttribute("page","reset");
            return "redirect:/login";
        }
        User u = userService.findByUsernameOrEmail(prt.getEmail());
        userService.changePassword(u.getUserId(), newPassword);
        resetStore.remove(token);
        ra.addFlashAttribute("signupSuccess","Đổi mật khẩu thành công! Đăng nhập lại.");
        return "redirect:/login";
    }

    /* ----------- LOGOUT THỦ CÔNG ----------- */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }
}
