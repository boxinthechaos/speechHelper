package org.example.speechhelper.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    public void sendEmail(String to, String title, String authCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("pjsleey12312@gmail.com");
            helper.setTo(to);
            helper.setSubject(title);

            String htmlContent = " <div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 16px; overflow: hidden;'>" +
                    "    <div style='background-color: #0f172a; padding: 40px 20px; text-align: center;'>" +
                    "        <h1 style='color: #10b981; margin: 0;'>AI 팩폭 면접관</h1>" +
                    "        <p style='color: #94a3b8; margin-top: 10px;'>당신의 도전을 환영합니다!</p>" +
                    "    </div>" +
                    "    <div style='padding: 40px 30px; background-color: #ffffff;'>" +
                    "        <h2 style='color: #1e293b; font-size: 20px; margin-bottom: 20px;'>이메일 인증을 완료해주세요</h2>" +
                    "        <p style='color: #475569; line-height: 1.6;'>회원가입을 위해 아래의 인증번호를 복사하여 가입창에 입력해주세요. 인증번호는 5분간 유효합니다.</p>" +
                    "        <div style='background-color: #f1f5f9; padding: 20px; text-align: center; border-radius: 12px; margin: 30px 0;'>" +
                    "            <span style='font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #3b82f6;'>" + authCode + "</span>" +
                    "        </div>" +
                    "        <p style='color: #64748b; font-size: 13px;'>본인이 요청하지 않은 경우 이 메일을 무시하셔도 됩니다.</p>" +
                    "    </div>" +
                    "    <div style='background-color: #f8fafc; padding: 20px; text-align: center; border-top: 1px solid #e2e8f0;'>" +
                    "        <p style='color: #94a3b8; font-size: 12px; margin: 0;'>&copy; 2026 SpeechHelper. All rights reserved.</p>" +
                    "    </div>" +
                    "</div>";

            helper.setText(htmlContent, true); // 두 번째 인자를 true로 설정해야 HTML로 전송됨

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 발송 중 오류가 발생했습니다.", e);
        }
    }
}