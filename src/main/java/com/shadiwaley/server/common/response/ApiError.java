package com.shadiwaley.server.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiError {
    private String field;
    private String code;
    private String message;
}