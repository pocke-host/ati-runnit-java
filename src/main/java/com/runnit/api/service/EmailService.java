package com.runnit.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
public class EmailService {

    // Optional — JavaMailSender is only auto-configured when spring.mail.host is set.
    // Without it (local dev, test env), emails are logged and skipped gracefully.
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@runnit.live}")
    private String fromAddress;

    @Value("${app.frontend.url:https://runnit.live}")
    private String frontendUrl;

    /**
     * Sends a password-reset email containing a one-time link.
     * The link expires in 60 minutes (controlled by AuthService token TTL).
     * If SMTP is not configured, logs the reset link and returns without throwing.
     */
    public void sendPasswordReset(String toEmail, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        if (mailSender == null) {
            log.warn("[email] SMTP not configured — skipping send. Reset link for {}: {}", toEmail, resetLink);
            return;
        }

        String html = buildPasswordResetHtml(resetLink);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Reset your RUNNIT password");
            helper.setText(html, true);
            mailSender.send(message);
            log.info("[email] Password reset sent to {}", toEmail);
        } catch (Exception e) {
            log.error("[email] Failed to send password reset to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send reset email", e);
        }
    }

    private String buildPasswordResetHtml(String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f5f5f5;font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f5f5f5;padding:40px 20px;">
                <tr><td align="center">
                  <table width="480" cellpadding="0" cellspacing="0" style="background:#fff;border:1px solid #e5e5e5;">
                    <!-- Header -->
                    <tr>
                      <td style="background:#000;padding:28px 40px;">
                        <span style="color:#fff;font-size:22px;font-weight:900;letter-spacing:0.1em;">RUNNIT</span>
                      </td>
                    </tr>
                    <!-- Body -->
                    <tr>
                      <td style="padding:40px;">
                        <p style="margin:0 0 16px;font-size:22px;font-weight:700;color:#000;">Reset your password</p>
                        <p style="margin:0 0 28px;font-size:15px;color:#555;line-height:1.6;">
                          We received a request to reset the password for your RUNNIT account.
                          Click the button below to choose a new password. This link expires in <strong>60 minutes</strong>.
                        </p>
                        <a href="%s"
                           style="display:inline-block;background:#0052FF;color:#fff;text-decoration:none;
                                  padding:14px 32px;font-size:14px;font-weight:700;letter-spacing:0.08em;
                                  text-transform:uppercase;">
                          Reset Password
                        </a>
                        <p style="margin:28px 0 0;font-size:13px;color:#999;line-height:1.5;">
                          If you didn't request a password reset, you can ignore this email — your password will stay the same.<br><br>
                          Or copy this link into your browser:<br>
                          <span style="color:#0052FF;word-break:break-all;">%s</span>
                        </p>
                      </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                      <td style="padding:20px 40px;border-top:1px solid #e5e5e5;">
                        <p style="margin:0;font-size:12px;color:#bbb;">© RUNNIT · runnit.live</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(resetLink, resetLink);
    }
}
