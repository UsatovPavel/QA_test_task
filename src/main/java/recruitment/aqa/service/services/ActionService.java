package recruitment.aqa.service.services;

import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import recruitment.aqa.service.aspects.Lock;
import recruitment.aqa.service.exceptions.TokenAlreadyExists;
import recruitment.aqa.service.exceptions.TokenNotFound;

@Service
public class ActionService {
   private final RestClient restClient;
   private final TokenStorage storage;

   public ActionService(RestClient restClient, TokenStorage tokenStorage) {
      this.restClient = restClient;
      this.storage = tokenStorage;
   }

   @Lock(
      key = "token"
   )
   public void auth(String token) {
      if (this.storage.contains(token)) {
         throw new TokenAlreadyExists(token);
      } else {
         LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap();
         body.add("token", token);
         ((RequestBodySpec)this.restClient.post().uri("/auth", new Object[0])).body(body).retrieve().toBodilessEntity();
         this.storage.put(token);
      }
   }

   @Lock(
      key = "token"
   )
   public void action(String token) {
      if (!this.storage.contains(token)) {
         throw new TokenNotFound(token);
      } else {
         LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap();
         body.add("token", token);
         ((RequestBodySpec)this.restClient.post().uri("/doAction", new Object[0])).body(body).retrieve().toBodilessEntity();
      }
   }

   @Lock(
      key = "token"
   )
   public void logout(String token) {
      boolean removed = this.storage.remove(token);
      if (!removed) {
         throw new TokenNotFound(token);
      }
   }
}
