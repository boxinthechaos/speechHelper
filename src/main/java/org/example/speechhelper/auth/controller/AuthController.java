package org.example.speechhelper.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.speechhelper.auth.dto.LoginRequestDto;
import org.example.speechhelper.auth.service.AuthService;
import org.example.speechhelper.auth.dto.SignUpRequestDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody SignUpRequestDto requestDto) {
        try {
            authService.signUp(requestDto);
            return ResponseEntity.ok("회원가입 성공");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequestDto requestDto,
                                        HttpServletResponse response) {
        Map<String, String> tokens = authService.login(requestDto);Cookie refreshCookie = new Cookie("refreshToken", tokens.get("refreshToken"));
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(14 * 24 * 60 * 60);
        refreshCookie.setSecure(true);
        response.addCookie(refreshCookie);

        Cookie accessCookie = new Cookie("accessToken", tokens.get("accessToken"));
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(60 * 30);
        response.addCookie(accessCookie);

        return ResponseEntity.ok("로그인 성공");
    }

    @GetMapping("/loginP")
    public String loginP(){
        return "login";
    }

    @GetMapping("/signupP")
    public String signupP(){
        return "signup";
    }

    @PostMapping("/email-verification")
    public ResponseEntity<String> sendEmailCode(@RequestParam String email) {
        try {
            authService.sendVerificationCode(email);
            return ResponseEntity.ok("인증 코드가 발송되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/email-verify")
    public ResponseEntity<String> verifyEmailCode(@RequestParam String email, @RequestParam String code) {
        try {
            authService.verifyCode(email, code);
            return ResponseEntity.ok("인증에 성공하였습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reissue")
    public ResponseEntity<String> reissue(@CookieValue(value = "refreshToken", required = false) String refreshToken,
                                          HttpServletResponse response) {

        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh Token이 없습니다. 다시 로그인해주세요.");
        }

        try {
            Map<String, String> tokens = authService.reissue(refreshToken);

            Cookie accessCookie = new Cookie("accessToken", tokens.get("accessToken"));
            accessCookie.setHttpOnly(true);
            accessCookie.setPath("/");
            accessCookie.setMaxAge(60 * 30);
            response.addCookie(accessCookie);

            if (tokens.containsKey("refreshToken")) {
                Cookie refreshCookie = new Cookie("refreshToken", tokens.get("refreshToken"));
                refreshCookie.setHttpOnly(true);
                refreshCookie.setPath("/");
                refreshCookie.setMaxAge(14 * 24 * 60 * 60);
                refreshCookie.setSecure(true);
                response.addCookie(refreshCookie);
            }

            return ResponseEntity.ok("토큰 재발급 성공");

        } catch (IllegalArgumentException e) { // 만료되었거나 유효하지 않은 Refresh Token인 경우
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 Refresh Token입니다.");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        // 쿠키 삭제
        Cookie accessCookie = new Cookie("accessToken", null);
        accessCookie.setMaxAge(0);
        accessCookie.setPath("/");
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setMaxAge(0);
        refreshCookie.setPath("/");
        response.addCookie(refreshCookie);

        return ResponseEntity.ok().build();
    }
}
