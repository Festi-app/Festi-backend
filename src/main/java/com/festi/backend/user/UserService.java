package com.festi.backend.user;

import com.festi.backend.common.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserDTO.Response getMe(String userId, UUID festivalId) {
        return UserDTO.Response.from(findUser(userId, festivalId));
    }

    public UserDTO.Response updateMe(String userId, UUID festivalId, UserDTO.UpdateRequest request) {
        User user = findUser(userId, festivalId);
        user.updateProfile(request.name(), request.phone());
        return UserDTO.Response.from(user);
    }

    @Transactional(readOnly = true)
    public List<UserDTO.Response> getUsersByRole(UUID festivalId, UserRole role) {
        return userRepository.findByFestivalIdAndRole(festivalId, role).stream()
                .map(UserDTO.Response::from)
                .toList();
    }

    private User findUser(String userId, UUID festivalId) {
        return userRepository.findByIdAndFestivalId(userId, festivalId)
                .orElseThrow(() -> new NotFoundException("User not found."));
    }
}
