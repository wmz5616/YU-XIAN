package com.yuxian.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String projectPath = System.getProperty("user.dir");
        String uploadPath = projectPath + File.separator + "uploads" + File.separator;

        registry.addResourceHandler("/images/avatars/**")
                .addResourceLocations("file:" + uploadPath + "avatars/");
    }
}
