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

        // 로그인 시 loveType 들어오면 업데이트
        if (request.getLoveType() != null) {
            user.setLoveType(request.getLoveType());
            userRepository.save(user);
        }

        return ApiResponse.success("로그인 성공", null);
    }

    // 3. 아이디 중복 확인 기능
    // true = 중복됨, false = 사용 가능

    public ApiResponse<Boolean> checkUserIdDuplicate(String userId) {

        boolean exists = userRepository.existsByUserId(userId);

        if (exists) {
            return ApiResponse.success("이미 사용 중인 아이디입니다.", true);  // true = 중복
        }

        return ApiResponse.success("사용 가능한 아이디입니다.", false); // false = 사용 가능
    }

    // 4. 사용자 탈퇴 기능

    public ApiResponse<Void> deleteUser(String userId) {

        User user = userRepository.findByUserId(userId).orElse(null);

        if (user == null) {
            return ApiResponse.fail("존재하지 않는 사용자입니다.");
        }

        // 삭제 처리
        userRepository.delete(user);

        return ApiResponse.success("탈퇴가 완료되었습니다.", null);
    }

    // 5. Love Type 업데이트 기능
    public ApiResponse<Void> updateLoveType(String userId, Integer loveType) {

        User user = userRepository.findByUserId(userId).orElse(null);

        if (user == null) {
            return ApiResponse.fail("존재하지 않는 사용자입니다.");
        }

        // loveType 유효성 검사 (0~15)
        if (loveType == null || loveType < 0 || loveType > 15) {
            return ApiResponse.fail("유효하지 않은 Love Type입니다. (0~15)");
        }

        user.setLoveType(loveType);
        userRepository.save(user);

        return ApiResponse.success("Love Type이 저장되었습니다.", null);
    }
}
