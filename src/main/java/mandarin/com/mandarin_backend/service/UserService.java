package mandarin.com.mandarin_backend.service;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.ApiResponse;
import mandarin.com.mandarin_backend.dto.LoginRequest;
import mandarin.com.mandarin_backend.dto.SignUpRequest;
import mandarin.com.mandarin_backend.entity.User;
import mandarin.com.mandarin_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 1. 회원가입 기능
    public ApiResponse<Void> signUp(SignUpRequest request) {

        // 아이디 중복 체크
        if (userRepository.existsByUserId(request.getUserId())) {
            return ApiResponse.fail("이미 사용 중인 아이디입니다.");
        }

        // User 엔티티 생성
        User user = User.builder()
                .userId(request.getUserId())
                .username(request.getUsername())
                .password(request.getPassword())
                .loveType(0) 
                .createdAt(LocalDateTime.now())
                .build();

        // DB 저장
        userRepository.save(user);

        return ApiResponse.success("회원가입 성공", null);
    }

    // 2. 로그인 기능 + loveType 업데이트
    public ApiResponse<Void> login(LoginRequest request) {

        User user = userRepository.findByUserId(request.getUserId())
                .orElse(null);

        if (user == null || !user.getPassword().equals(request.getPassword())) {
            return ApiResponse.fail("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        // 로그인 시 loveType 들어오면 저장
        if (request.getLoveType() != null) {
            user.setLoveType(request.getLoveType());
            userRepository.save(user);
        }

        return ApiResponse.success("로그인 성공", null);
    }

    // 3. 아이디 중복 확인 기능
    public ApiResponse<Boolean> checkUserIdDuplicate(String userId) {

        boolean exists = userRepository.existsByUserId(userId);

        if (exists) {
            return ApiResponse.success("이미 사용 중인 아이디입니다.", false);
        }
        return ApiResponse.success("사용 가능한 아이디입니다.", true);
    }
}
