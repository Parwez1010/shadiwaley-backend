package com.shadiwaley.server.media.infrastructure.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage.r2")
public class R2StorageProperties {

    private String bucketName;
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String region = "auto";
}