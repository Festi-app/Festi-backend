package com.festi.backend.user;

import com.festi.backend.common.exception.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserDTO.Response getMe(String userId, UUID festivalId) {
        return UserDTO.Response.from(findUser(userId, festivalId));
    }

    public UserDTO.Response updateMe(String userId, UUID festivalId, UserDTO.UpdateRequest request) {
        User user = findUser(userId, festivalId);
        user.updateProfile(request.name(), request.phone());
        return UserDTO.Response.from(user);
    }

    private User findUser(String userId, UUID festivalId) {
        return userRepository.findByIdAndFestivalId(userId, festivalId)
                .orElseThrow(() -> new NotFoundException("User not found."));
    }
}
