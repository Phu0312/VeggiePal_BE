package com.veggiepal.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.veggiepal.blog.configuration.VideoProperties;

@SpringBootApplication
@EnableConfigurationProperties(VideoProperties.class)
public class BlogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogServiceApplication.class, args);
    }
}
