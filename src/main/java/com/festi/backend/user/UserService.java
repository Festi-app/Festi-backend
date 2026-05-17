package com.festi.backend.user;

import com.festi.backend.common.exception.ConflictException;
import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.security.JwtTokenService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;

    public UserService(UserRepository userRepository, JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional(readOnly = true)
    public UserDTO.Response getMe(UUID userId) {
        return UserDTO.Response.from(findUser(userId));
    }

    public UserDTO.UpdateResponse updateMe(UUID userId, UserDTO.UpdateRequest request) {
        User user = findUser(userId);

        if (request.email() != null
                && !request.email().equals(user.getEmail())
                && userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email is already in use.");
        }

        user.updateProfile(request.email(), request.name(), request.phone());

        return new UserDTO.UpdateResponse(
                UserDTO.Response.from(user),
                jwtTokenService.issueAccessToken(user)
        );
    }

    public void deleteMe(UUID userId) {
        userRepository.delete(findUser(userId));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found."));
    }
}
