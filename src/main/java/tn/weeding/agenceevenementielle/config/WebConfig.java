package tn.weeding.agenceevenementielle.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
       // registry.addResourceHandler("/uploads/**")
        registry.addResourceHandler("/uploads/**", "/api/uploads/**")
               // .addResourceLocations("file:uploads/");
                .addResourceLocations("file:uploads/", "file:/app/uploads/");
    }
}
