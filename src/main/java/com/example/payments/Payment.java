package com.example.payments;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", uniqueConstraints = @UniqueConstraint(columnNames = "reference"))
public class Payment {
    @Id
    private UUID id;
    @Column(nullable = false, updatable = false, length = 64)
    private String reference;
    @Column(nullable = false, updatable = false, length = 15)
    private String phone;
    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, updatable = false, length = 3)
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Payment() { } // Required by JPA.

    public Payment(PaymentDtos.Create request) {
        id = UUID.randomUUID();
        reference = request.reference();
        phone = request.phone();
        amount = request.amount();
        currency = request.currency();
        status = PaymentStatus.PENDING;
        createdAt = Instant.now();
    }

    public boolean matches(PaymentDtos.Create request) {
        return phone.equals(request.phone()) && currency.equals(request.currency())
                && amount.compareTo(request.amount()) == 0;
    }

    public UUID getId() { return id; }
    public String getReference() { return reference; }
    public String getPhone() { return phone; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setStatus(PaymentStatus status) { this.status = status; }
}
