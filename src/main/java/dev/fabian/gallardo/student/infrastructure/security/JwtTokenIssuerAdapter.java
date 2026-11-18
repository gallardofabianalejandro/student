package dev.fabian.gallardo.student.infrastructure.security;

import dev.fabian.gallardo.student.domain.model.AppUser;
import dev.fabian.gallardo.student.domain.model.Role;
import dev.fabian.gallardo.student.domain.port.out.TokenIssuer;
import com.nacionservicios.emv.jwt.autoconfigure.JwtProperties;
import com.nacionservicios.emv.jwt.token.InvalidTokenException;
import com.nacionservicios.emv.jwt.token.JwtTokenService;
import java.time.Duration;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class JwtTokenIssuerAdapter implements TokenIssuer {

    private final JwtTokenService tokens;
    private final JwtProperties properties;

    JwtTokenIssuerAdapter(JwtTokenService tokens, JwtProperties properties) {
        this.tokens = tokens;
        this.properties = properties;
    }

    @Override
    public String issueAccessToken(AppUser user) {
        return tokens.issueAccessToken(user.username(), user.roles().stream().map(Role::name).sorted().toList());
    }

    @Override
    public String issueRefreshToken(AppUser user) {
        return tokens.issueRefreshToken(user.username());
    }

    @Override
    public Optional<String> usernameOfRefreshToken(String refreshToken) {
        try {
            return Optional.of(tokens.parseRefresh(refreshToken).subject());
        } catch (InvalidTokenException e) {
            return Optional.empty();
        }
    }

    @Override
    public Duration refreshTtl() {
        return properties.refreshTokenTtl();
    }
}
