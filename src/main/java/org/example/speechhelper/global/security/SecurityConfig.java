package org.example.speechhelper.global.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.speechhelper.global.config.RedisUtil;
import org.example.speechhelper.token.filter.JwtAuthenticationFilter;
import org.example.speechhelper.token.provider.TokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final TokenProvider tokenProvider;
    private final RedisUtil redisUtil;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/api/v1/interview/**", "/api/v1/**").hasRole("USER")
                        .anyRequest().authenticated()
                )

                .exceptionHandling(exception -> exception
                                .authenticationEntryPoint((request, response, authException) -> {
                                    String acceptHeader = request.getHeader("Accept");
                                    if (acceptHeader != null && acceptHeader.contains("text/html")) {
                                        response.sendRedirect("/api/v1/auth/loginP");
                                    }
                                    else {
                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                                        response.setContentType("application/json;charset=UTF-8");
                                        response.getWriter().write("{\"code\":\"TOKEN_EXPIRED\"}");
                                    }
                                })
                )
                .addFilterBefore(new JwtAuthenticationFilter(tokenProvider, redisUtil),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
