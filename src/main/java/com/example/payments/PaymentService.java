package com.example.payments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentStore store;
    private final PaymentGateway gateway;

    public PaymentService(PaymentStore store, PaymentGateway gateway) {
        this.store = store;
        this.gateway = gateway;
    }

    public PaymentDtos.View create(PaymentDtos.Create request) {
        try {
            // This transaction commits BEFORE the network call. The database arbitrates duplicates.
            store.insert(request);
        } catch (DataIntegrityViolationException duplicate) {
            Payment existing = store.get(request.reference());
            if (!existing.matches(request)) {
                throw new ApiException(HttpStatus.CONFLICT, "Reference reused with different payment details");
            }
            return PaymentDtos.View.from(existing); // Never initiate twice for the same reference.
        }

        try {
            gateway.initiate(request);
        } catch (RestClientException uncertainOutcome) {
            log.warn("Provider outcome unknown for reference={}", request.reference());
            store.markUnknown(request.reference());
        }
        return PaymentDtos.View.from(store.get(request.reference()));
    }

    public PaymentDtos.View get(String reference) {
        return PaymentDtos.View.from(store.get(reference));
    }

    public PaymentDtos.View callback(PaymentDtos.Callback callback) {
        return PaymentDtos.View.from(store.complete(callback));
    }
}
