package com.ktx.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcUploadConfig implements WebMvcConfigurer {

    private final String uploadDir;

    public WebMvcUploadConfig(@Value("${ktx.upload.dir:uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path rootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String uploadUri = rootPath.toUri().toString();
        if (!uploadUri.endsWith("/")) {
            uploadUri += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadUri)
                .setCachePeriod(3600);
    }
}
