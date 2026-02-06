package com.limehousehotel.protelapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API style (stateless)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)

                // CORS (so browser calls from limehousehotel.com can work)
                .cors(Customizer.withDefaults())

                // No HTML login page
                .formLogin(AbstractHttpConfigurer::disable)

                // Use HTTP Basic Auth for now
                .httpBasic(Customizer.withDefaults())

                .authorizeHttpRequests(auth -> auth
                        // allow health checks / basic non-sensitive endpoints
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/hello").permitAll()
                        // allow preflight requests (CORS)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // everything else requires auth
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // ✅ Put your real frontend origins here
        cfg.setAllowedOrigins(List.of(
                "https://limehousehotel.com",
                "https://www.limehousehotel.com"
        ));

        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
//        cfg.setExposedHeaders(List.of("WWW-Authenticate")); // optional
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }


}
