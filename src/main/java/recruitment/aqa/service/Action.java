package recruitment.aqa.service;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum Action {
   LOGIN,
   LOGOUT,
   ACTION;

   public static String allowedValues() {
      return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
   }
}
