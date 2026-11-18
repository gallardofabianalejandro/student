package dev.fabian.gallardo.student.api.rest;

import dev.fabian.gallardo.student.api.request.RegisterRequest;
import dev.fabian.gallardo.student.api.response.MeResponse;
import dev.fabian.gallardo.student.domain.model.AppUser;
import dev.fabian.gallardo.student.domain.model.Role;
import dev.fabian.gallardo.student.domain.port.in.RegisterUserUseCase;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Solo existe con app.auth.registration-enabled=true; cerrado, el endpoint ni siquiera está mapeado (404). */
@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(prefix = "app.auth", name = "registration-enabled", havingValue = "true")
public class RegistrationController {

    private final RegisterUserUseCase registerUseCase;

    public RegistrationController(RegisterUserUseCase registerUseCase) {
        this.registerUseCase = registerUseCase;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MeResponse register(@Valid @RequestBody RegisterRequest request) {
        AppUser user = registerUseCase.register(request.username(), request.password());
        return new MeResponse(user.username(), user.roles().stream().map(Role::name).sorted().toList());
    }
}
