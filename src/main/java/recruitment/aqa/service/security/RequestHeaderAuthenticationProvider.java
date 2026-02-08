package recruitment.aqa.service.security;

import io.micrometer.common.util.StringUtils;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

public class RequestHeaderAuthenticationProvider implements AuthenticationProvider {
   private static final Logger LOGGER = LoggerFactory.getLogger(RequestHeaderAuthenticationProvider.class);
   @Value("${secret:qazWSXedc}")
   private String apiAuthSecret;

   public Authentication authenticate(Authentication authentication) throws AuthenticationException {
      String actualKey = String.valueOf(authentication.getPrincipal());
      if (!StringUtils.isBlank(actualKey) && actualKey.equals(this.apiAuthSecret)) {
         return new PreAuthenticatedAuthenticationToken(authentication.getPrincipal(), null, new ArrayList());
      } else {
         LOGGER.warn("Authentication failed: invalid api secret. Expected: '{}' Actual: '{}'", this.apiAuthSecret, actualKey);
         throw new BadCredentialsException("Authentication failed: invalid api secret");
      }
   }

   public boolean supports(Class<?> authentication) {
      return authentication.equals(PreAuthenticatedAuthenticationToken.class);
   }
}
