package com.veggiepal.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class CloudinaryConfig {

    @Bean
    Cloudinary cloudinary(StorageProperties storageProperties) {

        // No network call here: empty credentials still build a client, and only uploads fail
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", storageProperties.cloudName(),
                "api_key", storageProperties.apiKey(),
                "api_secret", storageProperties.apiSecret(),
                "secure", true,
                // seconds; without it httpclient5 waits up to 3 minutes per connect/read
                "timeout", 20
        ));
    }
}
