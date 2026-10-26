package com.example.payments;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<PaymentDtos.View> create(@Valid @RequestBody PaymentDtos.Create request) {
        return ResponseEntity.accepted()
                .location(URI.create("/api/payments/" + request.reference()))
                .body(service.create(request));
    }

    @GetMapping("/{reference}")
    public PaymentDtos.View get(@PathVariable String reference) { return service.get(reference); }

    @PostMapping("/callback")
    public PaymentDtos.View callback(@Valid @RequestBody PaymentDtos.Callback callback) {
        return service.callback(callback);
    }
}
