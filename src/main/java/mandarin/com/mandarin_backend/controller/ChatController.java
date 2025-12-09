package mandarin.com.mandarin_backend.controller;

import mandarin.com.mandarin_backend.dto.*;
import mandarin.com.mandarin_backend.service.ChatService;
import mandarin.com.mandarin_backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ReportService reportService;

    /**
     * 시뮬레이션 기반 AI와 대화
     * POST /api/chat/send
     * 
     * simulationId를 통해 Simulation과 UserCharacter 정보를 조회하여
     * AI에게 컨텍스트(나이, 관계, 러브타입, 히스토리, 목적, 카테고리)를 전달합니다.
     * 
     * @param request simulationId(시뮬레이션 ID), userMessage(사용자 메시지), history(대화 내역)
     * @return AI 응답
     */
    @PostMapping("/send")
    public ResponseEntity<ChatResponseDto> sendMessage(@RequestBody ChatRequestDto request) {
        
        // 필수 파라미터 검증
        if (request.getSimulationId() == null) {
            throw new IllegalArgumentException("simulationId는 필수입니다.");
        }
        if (request.getUserMessage() == null || request.getUserMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("userMessage는 필수입니다.");
        }
        
        System.out.println("[Chat] 메시지 수신 - 시뮬레이션ID: " + request.getSimulationId() 
            + ", 메시지: " + request.getUserMessage());
        
        ChatResponseDto response = chatService.chat(
            request.getSimulationId(),
            request.getUserMessage(),
            request.getHistory()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * 대화 로그를 시나리오 유형에 따라 분석하여 보고서 생성 및 DB 저장
     * POST /api/chat/report
     * 
     * @param request simulationId(시뮬레이션 ID), chatLogs(대화 로그), userName(사용자 이름), targetName(상대방 이름), scenarioType(시나리오 유형)
     * @return 시뮬레이션 분석 보고서
     */
    @PostMapping("/report")
    public ResponseEntity<ReportResponseDto> createReport(@RequestBody ReportRequestDto request) {
        
        // 필수 파라미터 검증
        if (request.getSimulationId() == null) {
            throw new IllegalArgumentException("simulationId는 필수입니다.");
        }
        if (request.getChatLogs() == null || request.getChatLogs().isEmpty()) {
            throw new IllegalArgumentException("chatLogs는 필수입니다.");
        }
        if (request.getUserName() == null || request.getUserName().trim().isEmpty()) {
            throw new IllegalArgumentException("userName은 필수입니다.");
        }
        if (request.getTargetName() == null || request.getTargetName().trim().isEmpty()) {
            throw new IllegalArgumentException("targetName은 필수입니다.");
        }
        if (request.getScenarioType() == null || request.getScenarioType().trim().isEmpty()) {
            throw new IllegalArgumentException("scenarioType은 필수입니다.");
        }
        
        System.out.println("[Report] 보고서 요청 - 시뮬레이션ID: " + request.getSimulationId()
            + ", 사용자: " + request.getUserName() 
            + ", 대상: " + request.getTargetName()
            + ", 시나리오: " + request.getScenarioType());
        
        ReportResponseDto response = reportService.createReportAndSave(
            request.getSimulationId(),
            request.getChatLogs(),
            request.getUserName(),
            request.getTargetName(),
            request.getScenarioType()
        );
        
        return ResponseEntity.ok(response);
    }
}

