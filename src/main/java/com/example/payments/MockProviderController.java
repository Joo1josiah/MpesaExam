package com.example.payments;

import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@ConditionalOnProperty(name = "provider.mock-enabled", havingValue = "true")
public class MockProviderController {
    @PostMapping("/mock-provider/payments")
    public ResponseEntity<HttpPaymentGateway.ProviderReply> initiate(@Valid @RequestBody PaymentDtos.Create request)
            throws InterruptedException {
        // Deterministic training scenarios; no money moves and no provider credentials are needed.
        if (request.reference().startsWith("SLOW-")) { Thread.sleep(3000); }
        if (request.reference().startsWith("ERROR-")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return ResponseEntity.accepted().body(new HttpPaymentGateway.ProviderReply("ACCEPTED"));
    }
}
