package mandarin.com.mandarin_backend.service;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.SignUpRequest;
import mandarin.com.mandarin_backend.entity.User;
import mandarin.com.mandarin_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 회원가입 기능
    public void signUp(SignUpRequest request) {

        // 아이디 중복 체크
        if (userRepository.existsByUserId(request.getUserId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        // User 엔티티 생성
        User user = User.builder()
                .userId(request.getUserId())
                .username(request.getUsername())
                .password(request.getPassword())
                .loveType(request.getLoveType())
                .createdAt(LocalDateTime.now())
                .build();

        // DB 저장
        userRepository.save(user);
    }
}
