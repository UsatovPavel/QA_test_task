package recruitment.aqa.service.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.AuthorizedUrl;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.header.HeaderWriterFilter;
import recruitment.aqa.service.security.FailedAuthenticationEntryPoint;
import recruitment.aqa.service.security.RequestHeaderAuthenticationProvider;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
   @Bean
   protected RequestHeaderAuthenticationProvider requestHeaderAuthenticationProvider() {
      return new RequestHeaderAuthenticationProvider();
   }

   @Bean
   protected AuthenticationManager authenticationManager() {
      return new ProviderManager(Collections.singletonList(this.requestHeaderAuthenticationProvider()));
   }

   @Bean
   public RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter() {
      RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
      filter.setPrincipalRequestHeader("X-API-Key");
      filter.setExceptionIfHeaderMissing(false);
      filter.setAuthenticationManager(this.authenticationManager());
      return filter;
   }

   @Bean
   public SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
      FailedAuthenticationEntryPoint entrypoint = new FailedAuthenticationEntryPoint(objectMapper);
      http.cors(Customizer.withDefaults())
         .csrf(AbstractHttpConfigurer::disable)
         .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
         .addFilterAfter(this.requestHeaderAuthenticationFilter(), HeaderWriterFilter.class)
         .authorizeHttpRequests(registry -> ((AuthorizedUrl)registry.requestMatchers(new String[]{"/**"})).authenticated())
         .exceptionHandling(configurer -> configurer.authenticationEntryPoint(entrypoint));
      return (SecurityFilterChain)http.build();
   }
}
