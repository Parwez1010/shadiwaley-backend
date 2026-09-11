package com.shadiwaley.server.auth.dto.request;

import com.shadiwaley.server.user.domain.UserSide;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpInitiateRequest {

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone must be valid 10 digit Indian mobile number")
    private String phone;

    private UserSide side;

    private String guardianName;
}