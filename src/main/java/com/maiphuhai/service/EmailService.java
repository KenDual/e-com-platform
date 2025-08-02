package com.maiphuhai.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    /** địa chỉ from – lấy luôn username */
    @Value("${spring.mail.username}")
    private String from;

    public void sendOtp(String to, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject("[E-Com] Mã OTP đặt lại mật khẩu");
        msg.setText("""
                Xin chào,

                Mã OTP của bạn là: %s
                OTP có hiệu lực trong 10 phút.

                Nếu không phải bạn yêu cầu, hãy bỏ qua email này.
                """.formatted(otp));
        mailSender.send(msg);
    }
}
