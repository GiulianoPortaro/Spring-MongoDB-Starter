package com.mongodb.starter;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "starter")
@Getter
@Setter
public class StarterConfiguration {

    private String basePath;
    private String basePackage;
}
