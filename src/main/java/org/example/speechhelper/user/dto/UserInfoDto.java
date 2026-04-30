package org.example.speechhelper.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserInfoDto {
    private String name;
    private String email;
    private LocalDateTime createdAt;
}
