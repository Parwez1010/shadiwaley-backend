package com.shadiwaley.server.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class OtpVerifyRequest {

    @NotNull(message = "Temp token is required")
    private UUID tempToken;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone must be valid 10 digit Indian mobile number")
    private String phone;

    @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
    private String otp;
}