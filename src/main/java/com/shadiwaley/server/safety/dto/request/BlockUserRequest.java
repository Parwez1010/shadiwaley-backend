package com.shadiwaley.server.safety.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockUserRequest {
    @Size(max = 300)
    private String reason;
}