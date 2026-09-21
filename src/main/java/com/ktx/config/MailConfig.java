package com.ktx.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfig {

    private final boolean mailEnabled;

    public MailConfig(@Value("${ktx.mail.enabled:false}") boolean mailEnabled) {
        this.mailEnabled = mailEnabled;
    }

    public boolean isMailEnabled() {
        return mailEnabled;
    }
}
