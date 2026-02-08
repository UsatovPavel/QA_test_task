package recruitment.aqa.service.dtos;

import recruitment.aqa.service.Result;

public record UserResponse(Result result, String message) {
   public UserResponse(Result result) {
      this(result, null);
   }
}
