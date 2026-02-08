package recruitment.aqa.service.aspects;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import recruitment.aqa.service.services.TokenStorage;

@Aspect
@Component
public class LockingAspect {
   private final Cache<String, ReentrantLock> locks = Caffeine.newBuilder().expireAfterAccess(Duration.of(1L, ChronoUnit.MINUTES)).build();

   public LockingAspect(TokenStorage tokenStorage) {
   }

   @Around("@annotation(annotation)")
   public Object lock(ProceedingJoinPoint joinPoint, Lock annotation) throws Throwable {
      Object[] args = joinPoint.getArgs();
      MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
      String[] params = methodSignature.getParameterNames();
      String key = null;

      for (int i = 0; i < params.length; i++) {
         if (Objects.equals(params[i], annotation.key()) && args[i] instanceof String) {
            key = (String)args[i];
            break;
         }
      }

      if (key == null) {
         throw new Exception("Argument '" + annotation.key() + "' not found in " + methodSignature.getMethod());
      } else {
         MDC.put("token", key);
         ReentrantLock lock = this.locks.asMap().computeIfAbsent(key, r -> new ReentrantLock());
         lock.lock();

         Object var8;
         try {
            var8 = joinPoint.proceed();
         } finally {
            lock.unlock();
         }

         return var8;
      }
   }
}
