package org.example.speechhelper.user.service;

import lombok.RequiredArgsConstructor;
import org.example.speechhelper.user.dto.UserInfoDto;
import org.example.speechhelper.user.entity.User;
import org.example.speechhelper.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserInfoDto getMyInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        return new UserInfoDto(user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
