package dev.fabian.gallardo.student.infrastructure.config;

import com.nacionservicios.emv.jwt.autoconfigure.JwtProperties;
import com.nacionservicios.emv.jwt.filter.JwtAuthFilter;
import com.nacionservicios.emv.jwt.token.JwtTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Cadena stateless: access token en Authorization: Bearer; el refresh viaja en cookie HttpOnly. */
@Configuration
@ConditionalOnProperty(prefix = "app.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AuthProperties.class)
public class JwtSecurityConfig {

    @Bean
    SecurityFilterChain jwtFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter, JwtProperties jwt)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(jwt.excludedPaths().toArray(String[]::new))
                        .permitAll()
                        .requestMatchers("/error", "/actuator/health/**", "/actuator/info")
                        .permitAll()
                        // El shell del SPA y sus assets son publicos; la API sigue protegida.
                        .requestMatchers("/api/**")
                        .authenticated()
                        .anyRequest()
                        .permitAll())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Con SPA el filtro solo actua sobre /api/: el HTML y los assets se sirven sin token. */
    @Bean
    JwtAuthFilter jwtAuthFilter(JwtTokenService tokens, JwtProperties jwt) {
        return new JwtAuthFilter(tokens, jwt.excludedPaths()) {
            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                return !request.getRequestURI().startsWith("/api/") || super.shouldNotFilter(request);
            }
        };
    }

    /**
     * Las credenciales las valida AuthUseCaseImpl, no Spring Security. Declarar este bean evita que Boot cree su
     * usuario por defecto con contraseña aleatoria (que ni se usa ni debería existir).
     */
    @Bean
    UserDetailsService noUserDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Autenticación por usuario/contraseña no disponible aquí");
        };
    }

    /**
     * El filtro vive solo dentro de la cadena de seguridad. Si Boot además lo registrara como filtro de servlet,
     * correría fuera de la cadena y el contexto de seguridad que arma se perdería.
     */
    @Bean
    FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
