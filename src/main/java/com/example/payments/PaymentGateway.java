package com.example.payments;

public interface PaymentGateway {
    void initiate(PaymentDtos.Create request);
}
