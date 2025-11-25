package com.taskmanagement.notification.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import com.taskmanagement.notification.model.Notification;
import com.taskmanagement.notification.model.NotificationTemplate;
import com.taskmanagement.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final SendGrid sendGrid;
    private final NotificationTemplateRepository templateRepository;
    
    @Value("${sendgrid.from-email}")
    private String fromEmail;
    
    @Value("${sendgrid.from-name}")
    private String fromName;
    
    public void sendEmail(Notification notification) throws IOException {
        log.info("Sending email notification: {}", notification.getId());
        
        Email from = new Email(fromEmail, fromName);
        Email to = new Email(notification.getRecipientEmail());
        
        Mail mail;
        
        // Check if we should use a template
        if (notification.getTemplateId() != null) {
            mail = createTemplatedEmail(from, to, notification);
        } else {
            mail = createSimpleEmail(from, to, notification);
        }
        
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());
        
        Response response = sendGrid.api(request);
        
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            log.info("Email sent successfully for notification: {}", notification.getId());
        } else {
            log.error("Failed to send email for notification: {}. Status: {}, Body: {}", 
                    notification.getId(), response.getStatusCode(), response.getBody());
            throw new RuntimeException("Failed to send email: " + response.getBody());
        }
    }
    
    private Mail createSimpleEmail(Email from, Email to, Notification notification) {
        String subject = notification.getSubject() != null ? notification.getSubject() : "Task Management Notification";
        Content content = new Content("text/html", notification.getMessage());
        
        return new Mail(from, subject, to, content);
    }
    
    private Mail createTemplatedEmail(Email from, Email to, Notification notification) {
        Optional<NotificationTemplate> templateOpt = templateRepository.findById(notification.getTemplateId());
        
        if (templateOpt.isEmpty()) {
            log.warn("Template not found: {}, falling back to simple email", notification.getTemplateId());
            return createSimpleEmail(from, to, notification);
        }
        
        NotificationTemplate template = templateOpt.get();
        
        // Use SendGrid dynamic template if available
        if (template.getSendGridTemplateId() != null) {
            return createDynamicTemplateEmail(from, to, notification, template);
        } else {
            return createCustomTemplateEmail(from, to, notification, template);
        }
    }
    
    private Mail createDynamicTemplateEmail(Email from, Email to, Notification notification, NotificationTemplate template) {
        Mail mail = new Mail();
        mail.setFrom(from);
        mail.setTemplateId(template.getSendGridTemplateId());
        
        Personalization personalization = new Personalization();
        personalization.addTo(to);
        
        // Add template data
        if (notification.getTemplateData() != null) {
            for (Map.Entry<String, Object> entry : notification.getTemplateData().entrySet()) {
                personalization.addDynamicTemplateData(entry.getKey(), entry.getValue());
            }
        }
        
        // Add default data
        personalization.addDynamicTemplateData("subject", notification.getSubject());
        personalization.addDynamicTemplateData("message", notification.getMessage());
        
        mail.addPersonalization(personalization);
        
        return mail;
    }
    
    private Mail createCustomTemplateEmail(Email from, Email to, Notification notification, NotificationTemplate template) {
        String subject = processTemplate(template.getSubject(), notification.getTemplateData());
        String htmlContent = processTemplate(template.getHtmlContent(), notification.getTemplateData());
        
        Content content = new Content("text/html", htmlContent);
        
        return new Mail(from, subject, to, content);
    }
    
    private String processTemplate(String template, Map<String, Object> data) {
        if (template == null || data == null) {
            return template;
        }
        
        String processed = template;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            processed = processed.replace(placeholder, value);
        }
        
        return processed;
    }
    
    public void sendTestEmail(String recipientEmail, String subject, String message) throws IOException {
        log.info("Sending test email to: {}", recipientEmail);
        
        Email from = new Email(fromEmail, fromName);
        Email to = new Email(recipientEmail);
        Content content = new Content("text/html", message);
        
        Mail mail = new Mail(from, subject, to, content);
        
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());
        
        Response response = sendGrid.api(request);
        
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            log.info("Test email sent successfully to: {}", recipientEmail);
        } else {
            log.error("Failed to send test email to: {}. Status: {}, Body: {}", 
                    recipientEmail, response.getStatusCode(), response.getBody());
            throw new RuntimeException("Failed to send test email: " + response.getBody());
        }
    }
}