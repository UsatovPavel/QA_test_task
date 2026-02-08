package recruitment.aqa.service.configs;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

@Configuration
public class AppConfiguration {
   public static final String TOKEN = "token";

   @Bean
   public RestClient restClient(Builder builder) {
      return builder.build();
   }

   @Bean
   public RestClientCustomizer restClientCustomizer(@Value("${mock}") URI mock) {
      JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
      requestFactory.setReadTimeout(Duration.ofSeconds(10L));
      return builder -> builder.requestFactory(requestFactory)
         .baseUrl(mock)
         .defaultHeader("Accept", new String[]{"application/json"})
         .defaultHeader("Content-Type", new String[]{"application/x-www-form-urlencoded"})
         .build();
   }

   @Bean
   public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
      return new Jackson2ObjectMapperBuilder().failOnUnknownProperties(false).serializationInclusion(Include.NON_NULL);
   }
}
