package dev.fabian.gallardo.student.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.fabian.gallardo.student.application.usecase.AuthUseCaseImpl;
import dev.fabian.gallardo.student.domain.exception.InvalidCredentialsException;
import dev.fabian.gallardo.student.domain.exception.UsernameAlreadyExistsException;
import dev.fabian.gallardo.student.domain.model.AppUser;
import dev.fabian.gallardo.student.domain.model.Role;
import dev.fabian.gallardo.student.domain.port.out.PasswordHasher;
import dev.fabian.gallardo.student.domain.port.out.TokenIssuer;
import dev.fabian.gallardo.student.domain.port.out.UserRepository;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthUseCaseImplTest {

    private final Map<String, AppUser> store = new HashMap<>();

    private final UserRepository users = new UserRepository() {
        @Override
        public AppUser save(AppUser user) {
            if (store.containsKey(user.username())) {
                throw new UsernameAlreadyExistsException(user.username());
            }
            store.put(user.username(), user);
            return user;
        }

        @Override
        public Optional<AppUser> findByUsername(String username) {
            return Optional.ofNullable(store.get(username));
        }

        @Override
        public boolean existsByUsername(String username) {
            return store.containsKey(username);
        }
    };

    private final PasswordHasher hasher = new PasswordHasher() {
        @Override
        public String hash(String rawPassword) {
            return "h:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String hash) {
            return hash.equals("h:" + rawPassword);
        }
    };

    private final TokenIssuer tokens = new TokenIssuer() {
        @Override
        public String issueAccessToken(AppUser user) {
            return "access:" + user.username();
        }

        @Override
        public String issueRefreshToken(AppUser user) {
            return "refresh:" + user.username();
        }

        @Override
        public Optional<String> usernameOfRefreshToken(String refreshToken) {
            return refreshToken.startsWith("refresh:")
                    ? Optional.of(refreshToken.substring("refresh:".length()))
                    : Optional.empty();
        }

        @Override
        public Duration refreshTtl() {
            return Duration.ofDays(7);
        }
    };

    private final AuthUseCaseImpl useCase = new AuthUseCaseImpl(users, hasher, tokens);

    @Test
    void register_whenNewUser_thenStoresHashedPasswordAndUserRole() {
        var user = useCase.register("  Ana ", "s3cret-pass");

        assertThat(user.username()).isEqualTo("ana");
        assertThat(user.passwordHash()).isEqualTo("h:s3cret-pass");
        assertThat(user.roles()).containsExactly(Role.USER);
    }

    @Test
    void register_whenSameUsernameTwice_thenSecondIsRejected() {
        useCase.register("ana", "s3cret-pass");

        assertThatThrownBy(() -> useCase.register("ANA", "another-pass"))
                .isInstanceOf(UsernameAlreadyExistsException.class);
        assertThat(store.get("ana").passwordHash()).isEqualTo("h:s3cret-pass");
    }

    @Test
    void register_whenSameUsername_thenIdIsDeterministic() {
        var first = useCase.register("ana", "s3cret-pass");
        store.clear();

        var again = useCase.register("ana", "s3cret-pass");

        assertThat(again.id()).isEqualTo(first.id());
    }

    @Test
    void register_whenPasswordTooShort_thenRejected() {
        assertThatThrownBy(() -> useCase.register("ana", "short")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void register_whenUsernameBlank_thenRejected() {
        assertThatThrownBy(() -> useCase.register("   ", "s3cret-pass")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void login_whenCredentialsValid_thenReturnsTokens() {
        useCase.register("ana", "s3cret-pass");

        var result = useCase.login("Ana", "s3cret-pass");

        assertThat(result.accessToken()).isEqualTo("access:ana");
        assertThat(result.refreshToken()).isEqualTo("refresh:ana");
        assertThat(result.refreshTtl()).isEqualTo(Duration.ofDays(7));
    }

    @Test
    void login_whenWrongPassword_thenInvalidCredentials() {
        useCase.register("ana", "s3cret-pass");

        assertThatThrownBy(() -> useCase.login("ana", "wrong-pass")).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_whenUserDoesNotExist_thenSameInvalidCredentials() {
        assertThatThrownBy(() -> useCase.login("ghost", "whatever-pass")).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refresh_whenRefreshTokenValid_thenReturnsNewTokens() {
        useCase.register("ana", "s3cret-pass");

        var result = useCase.refresh("refresh:ana");

        assertThat(result.accessToken()).isEqualTo("access:ana");
    }

    @Test
    void refresh_whenTokenInvalid_thenInvalidCredentials() {
        assertThatThrownBy(() -> useCase.refresh("access:ana")).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refresh_whenUserWasRemoved_thenInvalidCredentials() {
        assertThatThrownBy(() -> useCase.refresh("refresh:ghost")).isInstanceOf(InvalidCredentialsException.class);
    }
}
