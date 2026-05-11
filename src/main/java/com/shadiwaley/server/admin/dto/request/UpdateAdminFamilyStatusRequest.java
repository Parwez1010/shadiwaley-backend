package com.shadiwaley.server.admin.dto.request;

import com.shadiwaley.server.profile.domain.ProfileStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAdminFamilyStatusRequest {

    @NotNull(message = "Profile status is required")
    private ProfileStatus profileStatus;
}