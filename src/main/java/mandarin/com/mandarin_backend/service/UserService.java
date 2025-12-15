package mandarin.com.mandarin_backend.service;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.ApiResponse;
import mandarin.com.mandarin_backend.dto.LoginRequest;
import mandarin.com.mandarin_backend.dto.SignUpRequest;
import mandarin.com.mandarin_backend.dto.UserResponseDto;
import mandarin.com.mandarin_backend.dto.UserUpdateRequestDto;
import mandarin.com.mandarin_backend.entity.User;
import mandarin.com.mandarin_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // 1. 회원가입 기능
    public ApiResponse<Long> signUp(SignUpRequest request) {

        // 아이디 중복 체크
        if (userRepository.existsByUserId(request.getUserId())) {
            return ApiResponse.fail("이미 사용 중인 아이디입니다.");
        }

        // User 엔티티 생성
        User user = User.builder()
                .userId(request.getUserId())
                .username(request.getUsername())
                .password(request.getPassword())
                .loveType(16)
                .createdAt(LocalDateTime.now())
                .build();

        // DB 저장
        User savedUser = userRepository.save(user);

        return ApiResponse.success("회원가입 성공", savedUser.getId());
    }

    // 2. 로그인 기능 + loveType 업데이트
    public ApiResponse<Long> login(LoginRequest request) {

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

        return ApiResponse.success("로그인 성공", user.getId());
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

    public ApiResponse<Void> deleteUser(Long id) {

        User user = userRepository.findById(id).orElse(null);

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

    // 6. 패스워드 확인 기능 (회원정보 수정 전 본인 확인용)
    public ApiResponse<Boolean> checkPassword(Long userId, String password) {

        if (userId == null || password == null) {
            return ApiResponse.fail("userId와 password는 필수입니다.");
        }

        User user = userRepository.findById(userId)
                .orElse(null);

        if (user == null) {
            return ApiResponse.fail("존재하지 않는 사용자입니다.");
        }

        // 패스워드 일치 여부 확인
        boolean isPasswordMatch = password.equals(user.getPassword());

        if (isPasswordMatch) {
            return ApiResponse.success("패스워드가 일치합니다.", true);
        } else {
            return ApiResponse.fail("패스워드가 일치하지 않습니다.");
        }
    }

    // 7. 회원 정보 조회 기능 (User 테이블의 기본키로 조회)
    public ApiResponse<UserResponseDto> getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElse(null);

        if (user == null) {
            return ApiResponse.fail("존재하지 않는 사용자입니다.");
        }

        // 비밀번호 제외하고 UserResponseDto로 변환
        UserResponseDto responseDto = UserResponseDto.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .username(user.getUsername())
                .loveType(user.getLoveType())
                .createdAt(user.getCreatedAt())
                .build();

        return ApiResponse.success("회원 정보 조회 성공", responseDto);
    }

    // 8. 회원 정보 수정 기능
    public ApiResponse<UserResponseDto> updateUser(Long id, UserUpdateRequestDto request) {

        User user = userRepository.findById(id)
                .orElse(null);

        if (user == null) {
            return ApiResponse.fail("존재하지 않는 사용자입니다.");
        }

        // 수정 가능한 필드 업데이트
        if (request.getUsername() != null && !request.getUsername().isEmpty()) {
            // 이름 유효성 검사 (한글 1~5자)
            if (!request.getUsername().matches("^[가-힣]{1,5}$")) {
                return ApiResponse.fail("이름은 공백 없이 한글 1~5자여야 합니다.");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            // 비밀번호 유효성 검사
            if (!request.getPassword().matches("^[A-Za-z0-9!@#$%^&*]{1,20}$")) {
                return ApiResponse.fail("비밀번호는 공백 없이 1~20자여야 합니다.");
            }
            user.setPassword(request.getPassword());
        }

        if (request.getLoveType() != null) {
            // loveType 유효성 검사 (0~15)
            if (request.getLoveType() < 0 || request.getLoveType() > 15) {
                return ApiResponse.fail("유효하지 않은 Love Type입니다. (0~15)");
            }
            user.setLoveType(request.getLoveType());
        }

        // DB 저장
        userRepository.save(user);

        // 수정된 정보를 UserResponseDto로 변환
        UserResponseDto responseDto = UserResponseDto.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .username(user.getUsername())
                .loveType(user.getLoveType())
                .createdAt(user.getCreatedAt())
                .build();

        return ApiResponse.success("회원 정보가 수정되었습니다.", responseDto);
    }
}
