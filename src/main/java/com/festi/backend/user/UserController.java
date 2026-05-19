package com.festi.backend.user;

import com.festi.backend.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO.Response> getMe(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(userService.getMe(currentUser.id(), currentUser.festivalId()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserDTO.Response> updateMe(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody UserDTO.UpdateRequest request
    ) {
        return ResponseEntity.ok(userService.updateMe(currentUser.id(), currentUser.festivalId(), request));
    }


}
