package com.example.payments;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class PaymentDtos {
    private PaymentDtos() { }

    public record Create(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String reference,
            @NotBlank @Pattern(regexp = "251[0-9]{9}") String phone,
            @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
            @NotBlank @Pattern(regexp = "ETB") String currency) { }

    public record Callback(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String reference,
            @NotNull PaymentStatus status) { }

    public record View(UUID id, String reference, String phone, BigDecimal amount,
                       String currency, PaymentStatus status, Instant createdAt) {
        public static View from(Payment p) {
            return new View(p.getId(), p.getReference(), p.getPhone(), p.getAmount(),
                    p.getCurrency(), p.getStatus(), p.getCreatedAt());
        }
    }
}
