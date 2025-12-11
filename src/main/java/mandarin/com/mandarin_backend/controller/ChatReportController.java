package mandarin.com.mandarin_backend.controller;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.ChatReportAvgResponseDto;
import mandarin.com.mandarin_backend.dto.ChatReportResponseDto;
import mandarin.com.mandarin_backend.service.ChatReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat/report")
public class ChatReportController {

    private final ChatReportService chatReportService;

    // 1. 리포트 단건 조회
    @GetMapping("/{chat_report_id}")
    public ResponseEntity<Map<String, Object>> getReportById(@PathVariable("chat_report_id") Integer id) {
        try {
            ChatReportResponseDto dto = chatReportService.getChatReportById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", dto);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // 2. 유저별 리포트 조회
    @GetMapping("/user/{id}")
    public ResponseEntity<Map<String, Object>> getReportsByUserId(@PathVariable("id") Long userId) {
        try {
            List<ChatReportResponseDto> list = chatReportService.getChatReportsByUserId(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", list);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // 3. 캐릭터별 리포트 조회
    @GetMapping("/character/{character_id}")
    public ResponseEntity<Map<String, Object>> getReportsByCharacterId(@PathVariable("character_id") Long characterId) {
        try {
            List<ChatReportResponseDto> list = chatReportService.getChatReportsByCharacterId(characterId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", list);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // 4. 유저별 리포트 평균 조회
    @GetMapping("/avg/{id}")
    public ResponseEntity<Map<String, Object>> getReportAvgByUserId(@PathVariable("id") Long userId) {
        try {
            List<ChatReportAvgResponseDto> list = chatReportService.getChatReportAvgByUserId(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", list);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
