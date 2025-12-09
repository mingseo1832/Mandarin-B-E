package mandarin.com.mandarin_backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import mandarin.com.mandarin_backend.dto.UserCharacterResponseDto;
import mandarin.com.mandarin_backend.exception.UserNotFoundException;
import mandarin.com.mandarin_backend.service.UserCharacterService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/character")
public class UserCharacterController {

    private final UserCharacterService userCharacterService;

    /**
     * 캐릭터 다건 조회
     * GET /character/user/{id}
     *
     * 성공:
     * {
     *   "code": 200,
     *   "data": [ { characterId: 1, characterName: "...", ... }, ... ]
     * }
     *
     * 유저 없음:
     * {
     *   "code": 400,
     *   "message": "회원 정보가 없습니다."
     * }
     */
    @GetMapping("/user/{id}")
    public ResponseEntity<Map<String, Object>> getCharactersByUserId(@PathVariable("id") Long userId) {

        List<UserCharacterResponseDto> characters = userCharacterService.getCharactersByUserId(userId);

        Map<String, Object> body = new HashMap<>();
        body.put("code", 200);
        body.put("data", characters);
        body.put("count", characters.size()); // 프론트에서 "캐릭터(5)" 표시할 때 사용 가능

        return ResponseEntity.ok(body);
    }

    // 이 컨트롤러에서 발생한 UserNotFoundException을 400으로 변환
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", 400);
        body.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }
}
