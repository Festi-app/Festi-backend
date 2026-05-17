package com.festi.backend.security;

import com.festi.backend.auth.AuthDTO;
import com.festi.backend.user.User;

public interface JwtTokenService {

    AuthDTO.TokenResponse issueAccessToken(User user);
}
