package org.example.speechhelper.auth.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.speechhelper.auth.dto.LoginRequestDto;
import org.example.speechhelper.auth.dto.SignUpRequestDto;
import org.example.speechhelper.token.provider.TokenProvider;
import org.example.speechhelper.user.entity.Role;
import org.example.speechhelper.user.entity.User;
import org.example.speechhelper.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    @Transactional
    public void signUp(SignUpRequestDto requestDto){
        if(userRepository.existsByEmail(requestDto.getEmail())){
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = new User();
        user.setEmail(requestDto.getEmail());
        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        user.setName(requestDto.getName());
        user.setRole(Role.USER);

        userRepository.save(user);
    }

    public Map<String, String> login(LoginRequestDto requestDto) {
        User user = userRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String accessToken = tokenProvider.createAccessToken(user.getEmail(), user.getRole());
        String refreshToken = tokenProvider.createRefreshToken(user.getEmail());

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);

        return tokens;
    }
}
