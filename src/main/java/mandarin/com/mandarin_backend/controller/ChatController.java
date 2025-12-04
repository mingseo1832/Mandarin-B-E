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
     * 페르소나를 적용한 AI와 대화
     * POST /api/chat/send
     * 
     * @param request persona(페르소나 정보), userMessage(사용자 메시지), history(대화 내역)
     * @return AI 응답
     */
    @PostMapping("/send")
    public ResponseEntity<ChatResponseDto> sendMessage(@RequestBody ChatRequestDto request) {
        
        System.out.println("[Chat] 메시지 수신: " + request.getUserMessage());
        
        ChatResponseDto response = chatService.chat(
            request.getPersona(),
            request.getUserMessage(),
            request.getHistory()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * 대화 로그를 분석하여 연애 관점 보고서 생성
     * POST /api/chat/report
     * 
     * @param request chatLogs(대화 로그), userName(사용자 이름), targetName(상대방 이름)
     * @return 연애 분석 보고서
     */
    @PostMapping("/report")
    public ResponseEntity<ReportResponseDto> createReport(@RequestBody ReportRequestDto request) {
        
        System.out.println("[Report] 보고서 요청 - 사용자: " + request.getUserName() + ", 대상: " + request.getTargetName());
        
        ReportResponseDto response = reportService.createReport(
            request.getChatLogs(),
            request.getUserName(),
            request.getTargetName()
        );
        
        return ResponseEntity.ok(response);
    }
}

