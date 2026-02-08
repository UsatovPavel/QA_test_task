package recruitment.aqa.service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import recruitment.aqa.service.Result;
import recruitment.aqa.service.dtos.UserResponse;

public class FailedAuthenticationEntryPoint implements AuthenticationEntryPoint {
   private final ObjectMapper objectMapper;

   public FailedAuthenticationEntryPoint(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
   }

   public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
      response.setStatus(401);
      response.setContentType("application/json");
      this.objectMapper.writeValue(response.getOutputStream(), new UserResponse(Result.ERROR, "Missing or invalid API Key"));
   }
}
