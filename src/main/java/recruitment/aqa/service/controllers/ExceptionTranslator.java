package recruitment.aqa.service.controllers;

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;
import recruitment.aqa.service.Action;
import recruitment.aqa.service.Result;
import recruitment.aqa.service.dtos.UserResponse;
import recruitment.aqa.service.exceptions.TokenAlreadyExists;
import recruitment.aqa.service.exceptions.TokenNotFound;

@RestControllerAdvice
public class ExceptionTranslator {
   private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionTranslator.class);

   @ResponseStatus(HttpStatus.BAD_REQUEST)
   @ExceptionHandler({MethodArgumentNotValidException.class})
   public UserResponse validationError(MethodArgumentNotValidException e) {
      String message = e.getBindingResult()
         .getFieldErrors()
         .stream()
         .map(
            fe -> "action".equals(fe.getField())
               ? "action: invalid action '%s'. Allowed: %s".formatted(fe.getRejectedValue(), Action.allowedValues())
               : fe.getField() + ": " + fe.getDefaultMessage()
         )
         .collect(Collectors.joining("; "));
      return new UserResponse(Result.ERROR, message);
   }

   @ResponseStatus(HttpStatus.CONFLICT)
   @ExceptionHandler({TokenAlreadyExists.class})
   public UserResponse tokenAlreadyExists(TokenAlreadyExists e) {
      return new UserResponse(Result.ERROR, e.getMessage());
   }

   @ResponseStatus(HttpStatus.FORBIDDEN)
   @ExceptionHandler({TokenNotFound.class})
   public UserResponse tokenNotFound(TokenNotFound e) {
      return new UserResponse(Result.ERROR, e.getMessage());
   }

   @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
   @ExceptionHandler({HttpStatusCodeException.class})
   public UserResponse httpClientError(HttpStatusCodeException e) {
      String s = MDC.get("token");
      LOGGER.warn("[token: {}] external server responded with error code {}", new Object[]{s, e.getStatusCode(), e});
      return new UserResponse(Result.ERROR, "Internal Server Error");
   }

   @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
   @ExceptionHandler({Exception.class})
   public UserResponse internalError(Exception e) {
      String s = MDC.get("token");
      LOGGER.error("[token: {}] {}", new Object[]{s, e.getMessage(), e});
      return new UserResponse(Result.ERROR, "Internal Server Error");
   }
}
