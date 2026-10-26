package com.example.payments;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {
    @Bean
    UserDetailsService users(@Value("${practice.username}") String username,
                             @Value("${practice.password}") String password) {
        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password("{bcrypt}" + new BCryptPasswordEncoder().encode(password)).roles("MERCHANT").build());
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http,
                                 @Value("${provider.callback-token}") String token) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/payments/callback", "/mock-provider/payments").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .addFilterBefore(new CallbackTokenFilter(token), BasicAuthenticationFilter.class)
                .build();
    }

    // Created inside the security chain, not as a servlet @Component (avoids double registration).
    static class CallbackTokenFilter extends OncePerRequestFilter {
        private final byte[] expected;
        CallbackTokenFilter(String token) { expected = token.getBytes(StandardCharsets.UTF_8); }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            if ("POST".equals(request.getMethod()) && "/api/payments/callback".equals(request.getServletPath())) {
                String supplied = request.getHeader("X-Callback-Token");
                if (supplied == null || !MessageDigest.isEqual(expected, supplied.getBytes(StandardCharsets.UTF_8))) {
                    response.setStatus(401);
                    response.setContentType("application/problem+json");
                    response.getWriter().write("{\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"Invalid callback token\"}");
                    return;
                }
            }
            chain.doFilter(request, response);
        }
    }
}
