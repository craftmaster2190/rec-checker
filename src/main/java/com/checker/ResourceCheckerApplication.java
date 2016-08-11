package com.checker;

import org.hibernate.validator.internal.constraintvalidators.hv.URLValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@SpringBootApplication
public class ResourceCheckerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResourceCheckerApplication.class, args);
	}
	
	@Bean
    public WebMvcConfigurerAdapter forwardToIndex() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                registry.addViewController("/").setViewName(
                        "forward:/index.html");
            }
        };
    }
	
	@Bean
	public URLValidator getURLValidator(){
		return new URLValidator();
	}
}
