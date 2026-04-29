package org.example.speechhelper.token.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.speechhelper.global.config.RedisUtil;
import org.example.speechhelper.token.provider.TokenProvider;
import org.example.speechhelper.user.entity.Role;
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
    private final RedisUtil redisUtil; // ← 추가

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String accessToken = resolveTokenFromCookie(request, "accessToken");

        if (accessToken != null && tokenProvider.validateToken(accessToken)) {
            // AccessToken 블랙리스트 체크 추가
            if (redisUtil.getData("BLACKLIST:" + accessToken) == null) {
                setAuthentication(accessToken);
            }
        } else {
            String refreshToken = resolveTokenFromCookie(request, "refreshToken");

            if (refreshToken != null && tokenProvider.validateToken(refreshToken)) {
                // RefreshToken 블랙리스트 체크 추가
                if (redisUtil.getData("BLACKLIST:" + refreshToken) != null) {
                    // 로그아웃된 토큰이면 쿠키 삭제 후 차단
                    expireCookie(response, "accessToken");
                    expireCookie(response, "refreshToken");
                    filterChain.doFilter(request, response);
                    return;
                }

                String email = tokenProvider.getEmailFromToken(refreshToken);
                String newAccessToken = tokenProvider.createAccessToken(email, Role.USER);

                Cookie newAccessCookie = new Cookie("accessToken", newAccessToken);
                newAccessCookie.setHttpOnly(true);
                newAccessCookie.setPath("/");
                newAccessCookie.setMaxAge(60);
                response.addCookie(newAccessCookie);
                setAuthentication(newAccessToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    // 쿠키 만료 헬퍼 메서드 추가
    private void expireCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

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