package com.festi.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Permit All
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/api/auth/signup", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/booth-applications").permitAll()
                        .requestMatchers(HttpMethod.GET, "/media/images/**").permitAll()

                        // All Authenticated Users
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/booths",
                                "/api/booths/*",
                                "/api/booths/*/menus",
                                "/api/locations",
                                "/api/festival",
                                "/api/festival/days",
                                "/api/festival/notices",
                                "/api/festival/timelines"
                        ).authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/users/me").authenticated()

                        // General User Only
                        .requestMatchers(HttpMethod.POST, "/api/favorites").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/favorites").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/favorites/*").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/booths/*/waitings").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/waitings/*").hasRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/waitings").hasRole("USER")
                        .requestMatchers(HttpMethod.POST, "/api/push-subscriptions").hasRole("USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/push-subscriptions/*").hasRole("USER")

                        // Booth Manager (+ Festival Admin passthrough)
                        .requestMatchers(HttpMethod.GET, "/api/booth-applications/me")
                        .hasAnyRole("BOOTH_MANAGER", "FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/booths/*")
                        .hasAnyRole("BOOTH_MANAGER", "FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/booths/*/image")
                        .hasAnyRole("BOOTH_MANAGER", "FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/booths/*/image")
                        .hasAnyRole("BOOTH_MANAGER", "FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/booths/*/menus")
                        .hasAnyRole("BOOTH_MANAGER", "FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/booths/*/menus/*")
                        .hasAnyRole("BOOTH_MANAGER", "FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/booths/*/menus/*")
                        .hasAnyRole("BOOTH_MANAGER", "FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/booths/*/menus/*/image")
                        .hasAnyRole("BOOTH_MANAGER", "FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/booths/*/menus/*/image")
                        .hasAnyRole("BOOTH_MANAGER", "FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/booths/*/menus/*/sold-out")
                        .hasAnyRole("BOOTH_MANAGER", "FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/booths/*/waitings")
                        .hasAnyRole("BOOTH_MANAGER", "FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/waitings/*/call")
                        .hasAnyRole("BOOTH_MANAGER", "FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/waitings/*/status")
                        .hasAnyRole("BOOTH_MANAGER", "FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/booths/*/waitings/status")
                        .hasAnyRole("BOOTH_MANAGER", "FESTIVAL_ADMIN")

                        // Festival Admin Only
                        .requestMatchers(HttpMethod.POST, "/api/booths/admin/food-trucks").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/booths/admin/food-trucks/*").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/booths/admin/food-trucks/*").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/festival").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/festival/days").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/festival/days/*").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/festival/days/*").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/festival/notices").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/festival/notices/*").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/festival/notices/*").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/festival/timelines").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/festival/timelines/*").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/festival/timelines/*").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/locations/slots").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/locations/*/assignment").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/locations/*/assignment").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/booth-applications").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/booth-applications/*").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/booth-applications/*/approve").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/booth-applications/*/reject").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/admin/booth-applications/*").hasRole("FESTIVAL_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/users").hasRole("FESTIVAL_ADMIN")


                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        return new JwtAuthenticationConverter();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
        return NimbusJwtEncoder.withSecretKey(secretKey(jwtProperties))
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
        return NimbusJwtDecoder.withSecretKey(secretKey(jwtProperties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${festi.cors.allowed-origins:http://localhost:3000}") String allowedOrigins
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        configuration.setAllowedMethods(Arrays.asList(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private SecretKey secretKey(JwtProperties jwtProperties) {
        return new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
