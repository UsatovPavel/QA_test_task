package recruitment.aqa.service.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import recruitment.aqa.service.Action;

public record UserRequest(@NotNull @Pattern(regexp = "^[0-9A-F]{32}$") String token, @NotNull Action action) {
}
