package mandarin.com.mandarin_backend.controller;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.ApiResponse;
import mandarin.com.mandarin_backend.dto.ChatReportResponseDto;
import mandarin.com.mandarin_backend.dto.LoveTypeRequestDto;
import mandarin.com.mandarin_backend.service.ReportService;
import mandarin.com.mandarin_backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final UserService userService;
    private final ReportService reportService;

    /**
     * 사용자의 설문 결과 기반 Love Type 저장 API
     * POST /report/love-type/{userId}
     */
    @PostMapping("/love-type/{userId}")
    public ResponseEntity<ApiResponse<Void>> saveLoveType(
            @PathVariable String userId,
            @RequestBody LoveTypeRequestDto request) {

        ApiResponse<Void> response = userService.updateLoveType(userId, request.getLoveType());

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 채팅 종료 후 AI 분석 리포트 조회 API (캐릭터 ID 기준)
     * GET /report/chat/{characterId}
     */
    @GetMapping("/chat/{characterId}")
    public ResponseEntity<ApiResponse<ChatReportResponseDto>> getChatReport(
            @PathVariable Long characterId) {

        ApiResponse<ChatReportResponseDto> response = reportService.getChatReport(characterId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 시뮬레이션 ID로 리포트 조회 API
     * GET /report/simulation/{simulationId}
     */
    @GetMapping("/simulation/{simulationId}")
    public ResponseEntity<ApiResponse<ChatReportResponseDto>> getChatReportBySimulation(
            @PathVariable Long simulationId) {

        ApiResponse<ChatReportResponseDto> response = reportService.getChatReportBySimulationId(simulationId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.badRequest().body(response);
    }
}
