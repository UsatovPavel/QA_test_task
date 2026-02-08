package recruitment.aqa.service.exceptions;

public class TokenNotFound extends RuntimeException {
   public TokenNotFound(String token) {
      super("Token '%s' not found".formatted(token));
   }
}
