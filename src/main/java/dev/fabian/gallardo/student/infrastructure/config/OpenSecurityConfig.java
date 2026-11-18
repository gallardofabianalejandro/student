package dev.fabian.gallardo.student.infrastructure.config;

import com.nacionservicios.emv.jwt.filter.JwtAuthFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Login apagado (app.auth.enabled=false): la API queda abierta. Es para desarrollo local; AuthDisabledGuard impide
 * arrancar asi con el perfil production.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.auth", name = "enabled", havingValue = "false")
public class OpenSecurityConfig {

    @Bean
    SecurityFilterChain openFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /** Evita que Boot cree su usuario por defecto con una contrasena aleatoria que nadie usa. */
    @Bean
    UserDetailsService noUserDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("El login esta apagado (app.auth.enabled=false)");
        };
    }

    /** El filtro JWT no debe correr como filtro de servlet suelto: con el login apagado no hay cadena que lo use. */
    @Bean
    FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(ObjectProvider<JwtAuthFilter> filter) {
        var registration = new FilterRegistrationBean<JwtAuthFilter>();
        var existing = filter.getIfAvailable();
        if (existing != null) {
            registration.setFilter(existing);
        }
        registration.setEnabled(false);
        return registration;
    }
}
