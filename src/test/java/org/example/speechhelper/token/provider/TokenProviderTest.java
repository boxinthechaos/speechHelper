package org.example.speechhelper.token.provider;

import org.example.speechhelper.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

class TokenProviderTest {

    private TokenProvider tokenProvider;

    // HS256은 최소 256비트(32바이트) 키가 필요합니다.
    private static final String SECRET = "test-secret-key-for-junit-must-be-32bytes!";
    private static final long ACCESS_VALIDITY  = 3600L;   // 1시간 (초)
    private static final long REFRESH_VALIDITY = 86400L;  // 24시간 (초)

    @BeforeEach
    void setUp() {
        tokenProvider = new TokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "secretKey",                     SECRET);
        ReflectionTestUtils.setField(tokenProvider, "accessTokenValidityInSeconds",  ACCESS_VALIDITY);
        ReflectionTestUtils.setField(tokenProvider, "refreshTokenValidityInSeconds", REFRESH_VALIDITY);
        tokenProvider.init();
    }

    // ──────────────────────────────────────────────
    // createAccessToken
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("createAccessToken()")
    class CreateAccessToken {

        @Test
        @DisplayName("생성된 토큰은 null이 아니고 비어 있지 않다")
        void tokenIsNotBlank() {
            String token = tokenProvider.createAccessToken("user@test.com", Role.USER);
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("토큰에서 이메일을 올바르게 추출할 수 있다")
        void extractsEmailCorrectly() {
            String token = tokenProvider.createAccessToken("user@test.com", Role.USER);
            assertThat(tokenProvider.getEmailFromToken(token)).isEqualTo("user@test.com");
        }

        @Test
        @DisplayName("생성된 토큰은 validateToken()을 통과한다")
        void tokenIsValid() {
            String token = tokenProvider.createAccessToken("user@test.com", Role.USER);
            assertThat(tokenProvider.validateToken(token)).isTrue();
        }
    }

    // ──────────────────────────────────────────────
    // createRefreshToken
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("createRefreshToken()")
    class CreateRefreshToken {

        @Test
        @DisplayName("생성된 리프레시 토큰은 null이 아니고 비어 있지 않다")
        void tokenIsNotBlank() {
            String token = tokenProvider.createRefreshToken("user@test.com");
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("리프레시 토큰에서 이메일을 올바르게 추출할 수 있다")
        void extractsEmailCorrectly() {
            String token = tokenProvider.createRefreshToken("user@test.com");
            assertThat(tokenProvider.getEmailFromToken(token)).isEqualTo("user@test.com");
        }

        @Test
        @DisplayName("리프레시 토큰은 validateToken()을 통과한다")
        void tokenIsValid() {
            String token = tokenProvider.createRefreshToken("user@test.com");
            assertThat(tokenProvider.validateToken(token)).isTrue();
        }
    }

    // ──────────────────────────────────────────────
    // validateToken
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("validateToken()")
    class ValidateToken {

        @Test
        @DisplayName("조작된 토큰은 false를 반환한다")
        void returnsFalseForTamperedToken() {
            String token = tokenProvider.createAccessToken("user@test.com", Role.USER);
            String tampered = token + "tampered";

            assertThat(tokenProvider.validateToken(tampered)).isFalse();
        }

        @Test
        @DisplayName("빈 문자열 토큰은 false를 반환한다")
        void returnsFalseForEmptyToken() {
            assertThat(tokenProvider.validateToken("")).isFalse();
        }

        @Test
        @DisplayName("완전히 다른 문자열은 false를 반환한다")
        void returnsFalseForGarbageToken() {
            assertThat(tokenProvider.validateToken("this.is.not.a.jwt")).isFalse();
        }

        @Test
        @DisplayName("만료된 토큰은 false를 반환한다")
        void returnsFalseForExpiredToken() throws Exception {
            // 유효기간 0초인 별도 TokenProvider 생성
            TokenProvider expiredProvider = new TokenProvider();
            ReflectionTestUtils.setField(expiredProvider, "secretKey",                     SECRET);
            ReflectionTestUtils.setField(expiredProvider, "accessTokenValidityInSeconds",  0L);
            ReflectionTestUtils.setField(expiredProvider, "refreshTokenValidityInSeconds", 0L);
            expiredProvider.init();

            String expiredToken = expiredProvider.createAccessToken("user@test.com", Role.USER);

            // 토큰 생성 직후 1ms 대기 (만료 보장)
            Thread.sleep(1);

            assertThat(tokenProvider.validateToken(expiredToken)).isFalse();
        }
    }

    // ──────────────────────────────────────────────
    // getEmailFromToken
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("getEmailFromToken()")
    class GetEmailFromToken {

        @Test
        @DisplayName("서로 다른 이메일로 만든 토큰은 이메일이 달라야 한다")
        void differentEmailsProduceDifferentTokens() {
            String tokenA = tokenProvider.createAccessToken("a@test.com", Role.USER);
            String tokenB = tokenProvider.createAccessToken("b@test.com", Role.USER);

            assertThat(tokenProvider.getEmailFromToken(tokenA)).isEqualTo("a@test.com");
            assertThat(tokenProvider.getEmailFromToken(tokenB)).isEqualTo("b@test.com");
        }
    }
}