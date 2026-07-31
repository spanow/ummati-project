package orga.takwa.ummati.config;

import orga.takwa.ummati.config.security.JwtAuthenticationFilter;
import orga.takwa.ummati.config.security.RateLimitingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, RateLimitingFilter rateLimitingFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/organizations/*/memberships/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/organizations/*/events").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/organizations/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/*/signups/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/*/documents").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/skills/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/public/**").permitAll()
                        // Images uploadées : seuls images/ et users/ sont montés statiquement
                        // (cf. WebMvcConfig) — les documents privés restent hors de portée.
                        .requestMatchers(HttpMethod.GET, "/uploads/images/**", "/uploads/users/**").permitAll()
                        // Swagger / Actuator
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**").permitAll()
                        // /actuator/health/** et non seulement /actuator/health : les sondes
                        // liveness et readiness sont sur des sous-chemins, et sans cette
                        // ouverture le healthcheck du conteneur reçoit un 403 et déclare
                        // l'application malsaine alors qu'elle fonctionne.
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        // Admin endpoints
                        .requestMatchers("/api/v1/admin/**").hasAuthority("PLATFORM_ADMIN")
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                        .frameOptions(fo -> fo.deny())
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Origines des webviews natives Capacitor.
     *
     * <p>Une app installée n'a pas de domaine : iOS sert l'app depuis
     * {@code capacitor://localhost} et Android depuis {@code http://localhost}. Ces
     * valeurs sont figées par le conteneur natif et ne peuvent pas être revendiquées
     * par un site distant, contrairement à un domaine — les inscrire en dur ici est
     * donc sans effet sur la surface d'attaque du web.
     */
    private static final List<String> NATIVE_APP_ORIGINS = List.of(
            "capacitor://localhost",
            "ionic://localhost",
            "http://localhost"
    );

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = new ArrayList<>(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .toList());
        NATIVE_APP_ORIGINS.stream()
                .filter(o -> !origins.contains(o))
                .forEach(origins::add);
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
