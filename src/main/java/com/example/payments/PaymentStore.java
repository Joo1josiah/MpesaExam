package com.example.payments;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentStore {
    private final PaymentRepository repository;

    public PaymentStore(PaymentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Payment insert(PaymentDtos.Create request) {
        return repository.saveAndFlush(new Payment(request));
    }

    @Transactional(readOnly = true)
    public Payment get(String reference) {
        return repository.findByReference(reference)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    @Transactional
    public Payment markUnknown(String reference) {
        Payment payment = locked(reference);
        // A callback may already have completed the payment while the HTTP call timed out.
        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.UNKNOWN);
        }
        return payment;
    }

    @Transactional
    public Payment complete(PaymentDtos.Callback callback) {
        if (callback.status() != PaymentStatus.SUCCESS && callback.status() != PaymentStatus.FAILED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Callback requires SUCCESS or FAILED");
        }
        Payment payment = locked(callback.reference());
        PaymentStatus previous = payment.getStatus();
        if ((previous == PaymentStatus.SUCCESS || previous == PaymentStatus.FAILED)
                && previous != callback.status()) {
            throw new ApiException(HttpStatus.CONFLICT, "Payment already has a different final result");
        }
        payment.setStatus(callback.status()); // Same-result callback is a safe no-op.
        return payment;
    }

    private Payment locked(String reference) {
        return repository.findLocked(reference)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));
    }
}
