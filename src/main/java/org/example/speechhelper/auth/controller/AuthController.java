package org.example.speechhelper.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.speechhelper.auth.dto.LoginRequestDto;
import org.example.speechhelper.auth.service.AuthService;
import org.example.speechhelper.auth.dto.SignUpRequestDto;
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
        authService.signUp(requestDto);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
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
}
