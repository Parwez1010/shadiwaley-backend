package com.shadiwaley.server.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class AuthUser {

    private AuthUser() {
    }

    public static UUID getCurrentUserId() {
        Authentication authentication = getAuthentication();
        return UUID.fromString(authentication.getPrincipal().toString());
    }

    public static UUID getCurrentActorId() {
        return getCurrentUserId();
    }

    public static ActorType getCurrentActorType() {
        Authentication authentication = getAuthentication();

        if (authentication.getDetails() instanceof ActorType actorType) {
            return actorType;
        }

        throw new IllegalArgumentException("Actor type is missing from security context");
    }

    public static boolean isEmployee() {
        return getCurrentActorType() == ActorType.EMPLOYEE;
    }

    public static boolean isCustomer() {
        return getCurrentActorType() == ActorType.CUSTOMER;
    }

    private static Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Unauthenticated user");
        }

        return authentication;
    }
}