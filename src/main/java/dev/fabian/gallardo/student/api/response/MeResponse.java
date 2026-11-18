package dev.fabian.gallardo.student.api.response;

import java.util.List;

public record MeResponse(String username, List<String> roles) {
}
