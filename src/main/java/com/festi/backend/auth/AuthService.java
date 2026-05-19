package com.festi.backend.auth;

import com.festi.backend.common.exception.ConflictException;
import com.festi.backend.common.exception.NotFoundException;
import com.festi.backend.festival.Festival;
import com.festi.backend.festival.FestivalRepository;
import com.festi.backend.security.JwtTokenService;
import com.festi.backend.user.User;
import com.festi.backend.user.UserRepository;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final FestivalRepository festivalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            UserRepository userRepository,
            FestivalRepository festivalRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.userRepository = userRepository;
        this.festivalRepository = festivalRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public User signup(AuthDTO.SignupRequest request) {
        Festival festival = detectFestival();

        if (userRepository.existsByIdAndFestivalId(request.id(), festival.getId())) {
            throw new ConflictException("ID is already in use.");
        }

        User user = new User(
                festival,
                request.id(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.phone()
        );
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AuthDTO.TokenResponse login(AuthDTO.LoginRequest request) {
        Festival festival = detectFestival();

        User user = userRepository.findByIdAndFestivalId(request.id(), festival.getId())
                .orElseThrow(() -> new BadCredentialsException("Invalid ID or password."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid ID or password.");
        }

        return jwtTokenService.issueAccessToken(user);
    }

    private Festival detectFestival() {
        List<Festival> festivals = festivalRepository.findAll();
        if (festivals.isEmpty()) {
            throw new NotFoundException("Festival not found.");
        }
        return festivals.getFirst();
    }
}
