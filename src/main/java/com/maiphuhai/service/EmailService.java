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

    /**
     * địa chỉ from – lấy luôn username
     */
    @Value("${spring.mail.username}")
    private String from;

    public void sendOtp(String to, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject("[E-Com] OTP Code for Password Reset");
        msg.setText("""
                Hello,
                
                Your OTP code is: %s
                The OTP is valid for 10 minutes.
                
                If you did not request this, please ignore this email.
                """.formatted(otp));
        mailSender.send(msg);
    }
}
