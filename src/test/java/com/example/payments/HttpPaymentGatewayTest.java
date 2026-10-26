package com.example.payments;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class HttpPaymentGatewayTest {
    @Test
    void sendsRealHttpRequestAndRejectsProviderErrors() throws Exception {
        AtomicReference<String> key = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/mock-provider/payments", exchange -> {
            key.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] reply = "{\"status\":\"ACCEPTED\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders("ERROR".equals(key.get()) ? 503 : 202, reply.length);
            exchange.getResponseBody().write(reply);
            exchange.close();
        });
        server.start();
        try {
            HttpPaymentGateway gateway = new HttpPaymentGateway("http://localhost:" + server.getAddress().getPort());
            gateway.initiate(new PaymentDtos.Create("ORDER-1", "251700000001", new BigDecimal("250.00"), "ETB"));
            assertThat(key.get()).isEqualTo("ORDER-1");
            assertThat(body.get()).contains("ORDER-1", "251700000001", "250.00");
            assertThatThrownBy(() -> gateway.initiate(
                    new PaymentDtos.Create("ERROR", "251700000001", BigDecimal.ONE, "ETB")))
                    .isInstanceOf(RestClientException.class);
        } finally {
            server.stop(0);
        }
    }
}
