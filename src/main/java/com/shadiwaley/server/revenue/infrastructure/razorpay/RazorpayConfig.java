package com.shadiwaley.server.revenue.infrastructure.razorpay;

import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RazorpayProperties.class)
public class RazorpayConfig {

    private final RazorpayProperties properties;

    @Bean
    public RazorpayClient razorpayClient() throws Exception {
        return new RazorpayClient(
                properties.getKeyId(),
                properties.getKeySecret()
        );
    }
}