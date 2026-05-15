package com.shadiwaley.server.media.dto.request;

import com.shadiwaley.server.media.domain.WhatsappConsent;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWhatsappConsentRequest {

    @NotNull(message = "WhatsApp consent is required")
    private WhatsappConsent whatsappConsent;
}