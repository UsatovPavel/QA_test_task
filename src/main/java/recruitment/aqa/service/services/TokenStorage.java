package recruitment.aqa.service.services;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class TokenStorage {
   private final Set<String> set = ConcurrentHashMap.newKeySet();

   public boolean contains(String token) {
      return this.set.contains(token);
   }

   public void put(String token) {
      this.set.add(token);
   }

   public boolean remove(String token) {
      return this.set.remove(token);
   }
}
