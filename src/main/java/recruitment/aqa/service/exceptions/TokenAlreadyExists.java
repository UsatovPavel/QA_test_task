package recruitment.aqa.service.exceptions;

public class TokenAlreadyExists extends RuntimeException {
   public TokenAlreadyExists(String token) {
      super("Token '%s' already exists".formatted(token));
   }
}
