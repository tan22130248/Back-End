package com.example.Back_End.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String to, String otp) {
        System.out.println("[EmailService] sendOtp called, to=" + to + ", fromEmail=" + fromEmail);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        if (fromEmail != null && !fromEmail.isBlank()) {
            message.setFrom(fromEmail);
        }
        message.setSubject("Mã OTP đặt lại mật khẩu AudioStory");
        message.setText("Mã OTP của bạn là: " + otp + "\nMã này có hiệu lực trong 5 phút.");
        System.out.println("[EmailService] Sending email via JavaMailSender...");
        try {
            mailSender.send(message);
            System.out.println("[EmailService] Email sent successfully.");
        } catch (Exception e) {
            System.out.println("[EmailService] FAILED to send email: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        System.out.println("[EmailService] sendHtmlEmail called, to=" + to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("[EmailService] HTML email sent successfully to " + to);
        } catch (Exception e) {
            System.out.println("[EmailService] FAILED to send HTML email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendPremiumConfirmedEmail(String to, String userName, String planName, String price) {
        String subject = "[AudioStory] Yêu cầu nâng cấp " + planName + " của bạn đã được xác nhận";
        boolean isVip = planName != null && planName.toLowerCase().contains("vip");
        String planLabel = isVip ? "VIP" : "Premium";
        StringBuilder html = new StringBuilder();
        html.append("<h2>Kính gửi ").append(userName != null ? userName : "").append(",</h2>")
            .append("<p>Yêu cầu nâng cấp gói <b>").append(planName).append("</b> (")
            .append(price != null ? price : "").append("/tháng) của bạn đã được <b>xác nhận</b>.</p>")
            .append("<p>Tài khoản của bạn đã được kích hoạt quyền lợi ").append(planLabel)
            .append(". Hãy đăng nhập và tận hưởng trải nghiệm nghe không giới hạn.</p>")
            .append("<p><b>Quyền lợi gói ").append(planLabel).append(" của bạn:</b></p>");

        if (isVip) {
            html.append("<p>- Tất cả quyền lợi của gói Premium:<br/>")
                .append("  + Không quảng cáo khi nghe audio<br/>")
                .append("  + Tạo danh sách phát cá nhân (các audio trong danh sách sẽ tự động phát tiếp sau khi phát hết mỗi audio, tối đa 3 audio)</p>")
                .append("<p>- Nghe tập mới sớm hơn (sẽ có mail thông báo riêng khi có tập mới)</p>")
                .append("<p>- Tạo danh sách phát cá nhân không giới hạn số lượng audio</p>");
        } else {
            html.append("<p>- Không quảng cáo khi nghe audio</p>")
                .append("<p>- Tạo danh sách phát cá nhân (các audio trong danh sách sẽ tự động phát tiếp sau khi phát hết mỗi audio, tối đa 3 audio)</p>");
        }

        html.append("<p>Nếu có vấn đề hoặc thắc mắc, vui lòng liên hệ: tannguyen.4420@gmail.com</p>")
            .append("<p>Trân trọng,<br/>Đội ngũ AudioStory</p>");

        sendHtmlEmail(to, subject, html.toString());
    }

    public void sendPremiumRejectedEmail(String to, String userName, String planName, String reason) {
        String subject = "[AudioStory] Yêu cầu nâng cấp " + planName + " của bạn đã bị từ chối";
        String html = "<h2>Kính gửi " + (userName != null ? userName : "") + ",</h2>"
            + "<p>Yêu cầu đăng ký gói <b>" + planName + "</b> của bạn đã bị <b>từ chối</b>.</p>"
            + (reason != null && !reason.isBlank() ? "<p><b>Lý do:</b> " + reason + "</p>" : "")
            + "<p>Vui lòng kiểm tra lại thông tin thanh toán hoặc liên hệ hỗ trợ nếu có thắc mắc.</p>"
            + "<p>Trân trọng,<br/>Đội ngũ AudioStory</p>";
        sendHtmlEmail(to, subject, html);
    }
}
