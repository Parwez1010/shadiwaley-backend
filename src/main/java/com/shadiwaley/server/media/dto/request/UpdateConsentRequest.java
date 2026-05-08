package com.shadiwaley.server.media.dto.request;

import com.shadiwaley.server.media.domain.WhatsappConsent;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateConsentRequest {

    @NotNull(message = "Whatsapp consent is required")
    private WhatsappConsent whatsappConsent;
}