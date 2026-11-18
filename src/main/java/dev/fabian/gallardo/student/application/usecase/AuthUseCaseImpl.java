package dev.fabian.gallardo.student.application.usecase;

import dev.fabian.gallardo.student.domain.exception.InvalidCredentialsException;
import dev.fabian.gallardo.student.domain.exception.UsernameAlreadyExistsException;
import dev.fabian.gallardo.student.domain.model.AppUser;
import dev.fabian.gallardo.student.domain.model.AuthTokens;
import dev.fabian.gallardo.student.domain.model.Role;
import dev.fabian.gallardo.student.domain.port.in.LoginUseCase;
import dev.fabian.gallardo.student.domain.port.in.RefreshTokenUseCase;
import dev.fabian.gallardo.student.domain.port.in.RegisterUserUseCase;
import dev.fabian.gallardo.student.domain.port.out.PasswordHasher;
import dev.fabian.gallardo.student.domain.port.out.TokenIssuer;
import dev.fabian.gallardo.student.domain.port.out.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Registro, login y renovación de tokens. Sin estado en memoria: todo vive en el repositorio y en los tokens. */
@Service
public class AuthUseCaseImpl implements RegisterUserUseCase, LoginUseCase, RefreshTokenUseCase {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_USERNAME_LENGTH = 50;

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final TokenIssuer tokens;

    public AuthUseCaseImpl(UserRepository users, PasswordHasher hasher, TokenIssuer tokens) {
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
    }

    @Override
    public AppUser register(String username, String rawPassword) {
        String name = normalize(username);
        if (name.isEmpty() || name.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("username inválido");
        }
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("la contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }
        if (users.existsByUsername(name)) {
            throw new UsernameAlreadyExistsException(name);
        }
        return users.save(new AppUser(idFor(name), name, hasher.hash(rawPassword), Set.of(Role.USER)));
    }

    @Override
    public AuthTokens login(String username, String rawPassword) {
        AppUser user = users.findByUsername(normalize(username)).orElseThrow(InvalidCredentialsException::new);
        if (rawPassword == null || !hasher.matches(rawPassword, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return issueFor(user);
    }

    @Override
    public AuthTokens refresh(String refreshToken) {
        String username = tokens.usernameOfRefreshToken(refreshToken).orElseThrow(InvalidCredentialsException::new);
        AppUser user = users.findByUsername(username).orElseThrow(InvalidCredentialsException::new);
        return issueFor(user);
    }

    private AuthTokens issueFor(AppUser user) {
        return new AuthTokens(tokens.issueAccessToken(user), tokens.issueRefreshToken(user), tokens.refreshTtl());
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    /** Id derivado del username: un re-run del registro produce la misma identidad, nunca un UUID aleatorio. */
    private static String idFor(String normalizedUsername) {
        return UUID.nameUUIDFromBytes(normalizedUsername.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
