package com.example.Back_End.config;

import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Normalizes SMTP values entered in hosting-provider environment variables.
 * Gmail displays app passwords in groups of four characters, but JavaMail must
 * authenticate with the spaces removed.
 */
@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(MailProperties properties) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.getHost());
        sender.setPort(properties.getPort());
        sender.setUsername(trimToEmpty(properties.getUsername()));
        sender.setPassword(trimToEmpty(properties.getPassword()).replaceAll("\\s+", ""));
        sender.setProtocol(properties.getProtocol());

        Properties javaMailProperties = new Properties();
        javaMailProperties.putAll(properties.getProperties());
        sender.setJavaMailProperties(javaMailProperties);
        return sender;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
