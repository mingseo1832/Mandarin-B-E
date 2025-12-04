package mandarin.com.mandarin_backend.controller;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.ApiResponse;
import mandarin.com.mandarin_backend.dto.LoginRequest;
import mandarin.com.mandarin_backend.dto.LoveTypeRequestDto;
import mandarin.com.mandarin_backend.dto.SignUpRequest;
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
    public ResponseEntity<ApiResponse<Void>> signUp(@RequestBody SignUpRequest request) {

        ApiResponse<Void> response = userService.signUp(request);

        // 성공 시 201 CREATED
        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        // 실패 시 400 Bad Request
        return ResponseEntity.badRequest().body(response);
    }

    // 2. 로그인 API
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(@RequestBody LoginRequest request) {

        ApiResponse<Void> response = userService.login(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response); // 200 OK
        }

        return ResponseEntity.badRequest().body(response); // 실패 시
    }

    // 3. 아이디 중복 확인 API
    // GET /user/check-id?userId=test123
    @GetMapping("/check-id")
    public ResponseEntity<ApiResponse<Boolean>> checkId(@RequestParam String userId) {

        ApiResponse<Boolean> response = userService.checkUserIdDuplicate(userId);

        // 성공 = 200 OK (중복 여부 data로 전달)
        return ResponseEntity.ok(response);
    }

    // 4. 유저 탈퇴 API
    // DELETE /user/{userId}
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String userId) {

        ApiResponse<Void> response = userService.deleteUser(userId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response); // 200 OK
        }

        return ResponseEntity.badRequest().body(response); // 실패 시 400
    }

    // 5. 러브타입 업데이트 API
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
