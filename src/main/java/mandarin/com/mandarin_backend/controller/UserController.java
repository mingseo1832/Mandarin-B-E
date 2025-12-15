package mandarin.com.mandarin_backend.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.ApiResponse;
import mandarin.com.mandarin_backend.dto.CheckPasswordRequest;
import mandarin.com.mandarin_backend.dto.LoveTypeRequestDto;
import mandarin.com.mandarin_backend.dto.SignUpRequest;
import mandarin.com.mandarin_backend.dto.UserResponseDto;
import mandarin.com.mandarin_backend.dto.UserUpdateRequestDto;
import mandarin.com.mandarin_backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 1. 회원가입 API
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Long>> signUp(@RequestBody SignUpRequest request) {

        ApiResponse<Long> response = userService.signUp(request);

        // 성공 시 201 CREATED
        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        // 실패 시 400 Bad Request
        return ResponseEntity.badRequest().body(response);
    }

    // 2. 아이디 중복 확인 API
    // GET /user/check-id?userId=test123
    @GetMapping("/check-id")
    public ResponseEntity<ApiResponse<Boolean>> checkId(@RequestParam String userId) {

        ApiResponse<Boolean> response = userService.checkUserIdDuplicate(userId);

        // 성공 = 200 OK (중복 여부 data로 전달)
        return ResponseEntity.ok(response);
    }

    // 4. 패스워드 확인 API (회원정보 수정 전 본인 확인용)
    // POST /user/checkpw (명세서: /users/checkpw)
    @PostMapping("/checkpw")
    public ResponseEntity<ApiResponse<Boolean>> checkPassword(@RequestBody CheckPasswordRequest request,
                                                               HttpSession session) {

        // 세션에서 로그인된 사용자 ID 가져오기
        Long userId = (Long) session.getAttribute("LOGIN_USER_ID");

        if (userId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("로그인이 필요합니다."));
        }

        ApiResponse<Boolean> response = userService.checkPassword(userId, request.getPassword());

        if (response.isSuccess()) {
            return ResponseEntity.ok(response); // 200 OK
        }

        return ResponseEntity.badRequest().body(response); // 실패 시 400
    }

    // 5. 회원 정보 조회 API
    // GET /user/{id} (명세서: /users/{id})
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable Long id) {

        ApiResponse<UserResponseDto> response = userService.getUserById(id);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response); // 200 OK
        }

        return ResponseEntity.badRequest().body(response); // 실패 시 400
    }

    // 6. 회원 정보 수정 API (본인 정보만 수정 가능)
    // POST /user/update/{id} (명세서: /users/update/{id})
    @PostMapping("/update/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUser(@PathVariable Long id,
                                                                   @RequestBody UserUpdateRequestDto request,
                                                                   HttpSession session) {

        // 세션에서 로그인된 사용자 ID 가져오기
        Long loginUserId = (Long) session.getAttribute("LOGIN_USER_ID");

        if (loginUserId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("로그인이 필요합니다."));
        }

        ApiResponse<UserResponseDto> response = userService.updateUser(id, request, loginUserId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response); // 200 OK
        }

        return ResponseEntity.badRequest().body(response); // 실패 시 400
    }

    // 7. 유저 탈퇴 API
    // DELETE /user/delete/{id} (명세서: /users/delete/{id})
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {

        ApiResponse<Void> response = userService.deleteUser(id);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response); // 200 OK
        }

        return ResponseEntity.badRequest().body(response); // 실패 시 400
    }

    // 8. 러브타입 업데이트 API
    // PUT /user/lovetype
    @PutMapping("/lovetype")
    public ResponseEntity<ApiResponse<Void>> updateLoveType(@RequestBody LoveTypeRequestDto request) {

        ApiResponse<Void> response = userService.updateLoveType(request.getUserId(), request.getLoveType());

        if (response.isSuccess()) {
            return ResponseEntity.ok(response); // 200 OK
        }

        return ResponseEntity.badRequest().body(response); // 실패 시 400
    }
}
