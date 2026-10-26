package com.example.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpPaymentGateway implements PaymentGateway {
    private final RestClient client;

    public HttpPaymentGateway(@Value("${provider.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public void initiate(PaymentDtos.Create request) {
        ProviderReply reply = client.post().uri("/mock-provider/payments")
                .header("Idempotency-Key", request.reference())
                .body(request).retrieve().body(ProviderReply.class);
        if (reply == null || !"ACCEPTED".equals(reply.status())) {
            throw new RestClientException("Unexpected provider acknowledgement");
        }
    }

    public record ProviderReply(String status) { }
}
