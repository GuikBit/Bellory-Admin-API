package org.exemplo.belloryadmin.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

/**
 * EmailService minimalista do Bellory-Admin-API.
 *
 * <p>Versao enxuta: apenas {@code enviarEmailSimples(to, subject, body)}.
 * Templates Thymeleaf, anexos e demais funcionalidades do EmailService do
 * Bellory-API sao usadas nos fluxos do cliente (notificacoes/lembretes), que
 * ficam no app antigo.</p>
 */
@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${email.from.address}")
    private String fromAddress;

    @Value("${email.from.name}")
    private String fromName;

    /**
     * Envia email simples (texto puro) - usado pelo WebhookEventProcessor para
     * notificar a organizacao de eventos de pagamento.
     */
    @Async
    public void enviarEmailSimples(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            log.info("Email enviado para {}: {}", to, subject);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Falha ao enviar email para {}: {}", to, e.getMessage(), e);
        }
    }
}
