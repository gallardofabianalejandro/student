package dev.fabian.gallardo.student.api.response;

/** El refresh token nunca va en el cuerpo: viaja en una cookie HttpOnly. */
public record AccessTokenResponse(String accessToken) {
}
