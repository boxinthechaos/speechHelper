package org.example.speechhelper.auth.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.speechhelper.auth.dto.LoginRequestDto;
import org.example.speechhelper.auth.dto.SignUpRequestDto;
import org.example.speechhelper.global.config.RedisUtil;
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
    private final EmailService emailService;
    private final RedisUtil redisUtil;

    public void sendVerificationCode(String email){
        if(userRepository.existsByEmail(email)){
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        String code = String.valueOf((int)(Math.random() * 899999) + 100000);
        redisUtil.setDataExpire("AUTH_CODE:" + email, code, 5 * 60 * 1000L);
        emailService.sendEmail(email, "[SpeechHelper] 인증 코드", "인증 코드: " + code);
    }

    public void verifyCode(String email, String code){
        String savedCode = redisUtil.getData("AUTH_CODE:" + email);

        if (savedCode == null || !savedCode.equals(code)) {
            throw new IllegalArgumentException("인증 코드가 틀렸거나 만료되었습니다.");
        }

        redisUtil.setDataExpire("VERIFIED:" + email, "TRUE", 10 * 60 * 1000L);
        redisUtil.deleteData("AUTH_CODE:" + email);
    }

    @Transactional
    public void signUp(SignUpRequestDto requestDto){
        String isVerified = redisUtil.getData("VERIFIED:" + requestDto.getEmail());
        if (isVerified == null || !isVerified.equals("TRUE")) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
        }

        if(userRepository.existsByEmail(requestDto.getEmail())){
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = new User();
        user.setEmail(requestDto.getEmail());
        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        user.setName(requestDto.getName());
        user.setRole(Role.USER);

        userRepository.save(user);

        redisUtil.deleteData("VERIFIED:" + requestDto.getEmail());
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

    public Map<String, String> reissue(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 Refresh Token입니다.");
        }

        if (redisUtil.getData("BLACKLIST:" + refreshToken) != null) {
            throw new IllegalArgumentException("로그아웃된 토큰입니다.");
        }

        String email = tokenProvider.getEmailFromToken(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        String newAccessToken = tokenProvider.createAccessToken(user.getEmail(), user.getRole());
        String newRefreshToken = tokenProvider.createRefreshToken(user.getEmail());

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", newRefreshToken);

        return tokens;
    }

    public void logout(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 Refresh Token입니다.");
        }

        long expiration = tokenProvider.getExpiration(refreshToken);
        redisUtil.setDataExpire("BLACKLIST:" + refreshToken, "logout", expiration);
    }
}
