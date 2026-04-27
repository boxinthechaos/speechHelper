package org.example.speechhelper.token.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.speechhelper.token.provider.TokenProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final TokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. Access Token 꺼내기
        String accessToken = resolveTokenFromCookie(request, "accessToken");

        // 2. Access Token이 살아있다면? -> 정상 인증 처리
        if (accessToken != null && tokenProvider.validateToken(accessToken)) {
            setAuthentication(accessToken);
        }
        // 3. Access Token이 없거나 만료되었다면? -> Refresh Token 확인!
        else {
            String refreshToken = resolveTokenFromCookie(request, "refreshToken");

            // Refresh Token이 존재하고 유효하다면 인공호흡 시작
            if (refreshToken != null && tokenProvider.validateToken(refreshToken)) {
                String email = tokenProvider.getEmailFromToken(refreshToken);
                String newAccessToken = tokenProvider.createAccessToken(email, org.example.speechhelper.user.entity.Role.USER);

                Cookie newAccessCookie = new Cookie("accessToken", newAccessToken);
                newAccessCookie.setHttpOnly(true);
                newAccessCookie.setPath("/");
                newAccessCookie.setMaxAge(60);
                //newAccessCookie.setMaxAge(60 * 30);
                response.addCookie(newAccessCookie);
                setAuthentication(newAccessToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    // 인증 처리를 담당하는 중복 로직을 분리한 헬퍼 메서드
    private void setAuthentication(String token) {
        String email = tokenProvider.getEmailFromToken(token);
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(email, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveTokenFromCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}