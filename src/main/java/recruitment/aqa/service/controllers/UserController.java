package recruitment.aqa.service.controllers;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import recruitment.aqa.service.Result;
import recruitment.aqa.service.dtos.UserRequest;
import recruitment.aqa.service.dtos.UserResponse;
import recruitment.aqa.service.services.ActionService;

@RestController
public class UserController {
   private final ActionService actionService;

   public UserController(ActionService actionService) {
      this.actionService = actionService;
   }

   @PostMapping(
      value = {"/endpoint"},
      consumes = {"application/x-www-form-urlencoded"},
      produces = {"application/json"}
   )
   public UserResponse endpoint(@Valid @ModelAttribute UserRequest request) {
      switch (request.action()) {
         case LOGIN:
            this.actionService.auth(request.token());
            break;
         case ACTION:
            this.actionService.action(request.token());
            break;
         case LOGOUT:
            this.actionService.logout(request.token());
      }

      return new UserResponse(Result.OK);
   }
}
