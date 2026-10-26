package com.example.payments;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClientException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:payment-tests;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "practice.username=student", "practice.password=practice-only",
        "provider.callback-token=local-callback-token"
})
class PaymentApiTest {
    @LocalServerPort int port;
    @MockitoBean PaymentGateway gateway;
    @Autowired PaymentStore store;
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void createsReadsAndCompletesPayment() throws Exception {
        String ref = reference();
        assertThat(create(ref, "250.00").statusCode()).isEqualTo(202);
        assertThat(send("GET", "/api/payments/" + ref, null, true, null).body()).contains("PENDING");
        assertThat(callback(ref, "SUCCESS", "local-callback-token").body()).contains("SUCCESS");
        assertThat(callback(ref, "SUCCESS", "local-callback-token").statusCode()).isEqualTo(200);
        assertThat(callback(ref, "FAILED", "local-callback-token").statusCode()).isEqualTo(409);
        verify(gateway, times(1)).initiate(any());
    }

    @Test
    void sameReferenceReplaysButChangedPayloadConflicts() throws Exception {
        String ref = reference();
        assertThat(create(ref, "250.00").statusCode()).isEqualTo(202);
        assertThat(create(ref, "250.0").statusCode()).isEqualTo(202);
        assertThat(create(ref, "999.00").statusCode()).isEqualTo(409);
        verify(gateway, times(1)).initiate(any());
    }

    @Test
    void concurrentDuplicateRequestsInitiateOnlyOnce() throws Exception {
        String ref = reference();
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            Callable<Integer> task = () -> { start.await(); return create(ref, "10.00").statusCode(); };
            Future<Integer> first = executor.submit(task);
            Future<Integer> second = executor.submit(task);
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo(202);
            assertThat(second.get(10, TimeUnit.SECONDS)).isEqualTo(202);
        }
        verify(gateway, times(1)).initiate(any());
    }

    @Test
    void providerFailureBecomesUnknownAndReplayDoesNotResubmit() throws Exception {
        doThrow(new RestClientException("Timeout")).when(gateway).initiate(any());
        String ref = reference();
        assertThat(create(ref, "5.00").body()).contains("UNKNOWN");
        assertThat(create(ref, "5.00").body()).contains("UNKNOWN");
        assertThat(callback(ref, "SUCCESS", "local-callback-token").body()).contains("SUCCESS");
        verify(gateway, times(1)).initiate(any());
    }

    @Test
    void lateHttpFailureCannotOverwriteAnEarlierSuccessfulCallback() throws Exception {
        doAnswer(invocation -> {
            PaymentDtos.Create request = invocation.getArgument(0);
            store.complete(new PaymentDtos.Callback(request.reference(), PaymentStatus.SUCCESS));
            throw new RestClientException("Response was lost after callback");
        }).when(gateway).initiate(any());
        assertThat(create(reference(), "5.00").body()).contains("SUCCESS");
    }

    @Test
    void invalidInputsAndMissingPaymentReturnUsefulErrors() throws Exception {
        for (String amount : List.of("0", "-1", "1.001")) {
            assertThat(create(reference(), amount).statusCode()).isEqualTo(400);
        }
        assertThat(send("POST", "/api/payments", "{broken", true, null).statusCode()).isEqualTo(400);
        assertThat(send("POST", "/api/payments", "{\"reference\":\"A\"}", true, null).statusCode()).isEqualTo(400);
        assertThat(send("GET", "/api/payments/missing", null, true, null).statusCode()).isEqualTo(404);
        verifyNoInteractions(gateway);
    }

    @Test
    void authenticationAndCallbackStateAreEnforced() throws Exception {
        assertThat(send("GET", "/api/payments/anything", null, false, null).statusCode()).isEqualTo(401);
        String ref = reference();
        create(ref, "1.00");
        assertThat(callback(ref, "SUCCESS", null).statusCode()).isEqualTo(401);
        assertThat(callback(ref, "SUCCESS", "wrong").statusCode()).isEqualTo(401);
        assertThat(callback(ref, "PENDING", "local-callback-token").statusCode()).isEqualTo(400);
        assertThat(callback(ref, "NOT_A_STATE", "local-callback-token").statusCode()).isEqualTo(400);
        assertThat(callback("missing", "SUCCESS", "local-callback-token").statusCode()).isEqualTo(404);
        assertThat(callback(ref, "FAILED", "local-callback-token").body()).contains("FAILED");
    }

    private String reference() { return "ORDER-" + UUID.randomUUID(); }

    private HttpResponse<String> create(String ref, String amount) throws Exception {
        String body = "{\"reference\":\"%s\",\"phone\":\"251700000001\",\"amount\":%s,\"currency\":\"ETB\"}".formatted(ref, amount);
        return send("POST", "/api/payments", body, true, null);
    }

    private HttpResponse<String> callback(String ref, String status, String token) throws Exception {
        return send("POST", "/api/payments/callback",
                "{\"reference\":\"%s\",\"status\":\"%s\"}".formatted(ref, status), false, token);
    }

    private HttpResponse<String> send(String method, String path, String body, boolean auth, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (auth) {
            builder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                    "student:practice-only".getBytes(StandardCharsets.UTF_8)));
        }
        if (token != null) { builder.header("X-Callback-Token", token); }
        return http.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
