package dev.fabian.gallardo.student.api.rest;

import dev.fabian.gallardo.student.api.request.LoginRequest;
import dev.fabian.gallardo.student.api.response.AccessTokenResponse;
import dev.fabian.gallardo.student.api.response.MeResponse;
import dev.fabian.gallardo.student.domain.model.AuthTokens;
import dev.fabian.gallardo.student.domain.port.in.LoginUseCase;
import dev.fabian.gallardo.student.domain.port.in.RefreshTokenUseCase;
import dev.fabian.gallardo.student.infrastructure.config.AuthProperties;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sin lógica de negocio: delega en los use cases y arma la cookie del refresh token. */
@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(prefix = "app.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthController {

    static final String REFRESH_COOKIE = "refresh_token";
    private static final String COOKIE_PATH = "/api/auth";

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshUseCase;
    private final AuthProperties properties;

    public AuthController(LoginUseCase loginUseCase, RefreshTokenUseCase refreshUseCase, AuthProperties properties) {
        this.loginUseCase = loginUseCase;
        this.refreshUseCase = refreshUseCase;
        this.properties = properties;
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return withRefreshCookie(loginUseCase.login(request.username(), request.password()));
    }

    /**
     * El header X-Requested-With es obligatorio: un formulario de otro sitio no puede enviarlo, lo que cubre
     * CSRF en el único endpoint que se autentica con una cookie.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE) String refreshToken,
            @RequestHeader(name = "X-Requested-With") String requestedWith) {
        return withRefreshCookie(refreshUseCase.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .build();
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring("ROLE_".length()) : a)
                .sorted()
                .toList();
        return new MeResponse(authentication.getName(), roles);
    }

    private ResponseEntity<AccessTokenResponse> withRefreshCookie(AuthTokens tokens) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken(), tokens.refreshTtl()).toString())
                .body(new AccessTokenResponse(tokens.accessToken()));
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(properties.secureCookie())
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
