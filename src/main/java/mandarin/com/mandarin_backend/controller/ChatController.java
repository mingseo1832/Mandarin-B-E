package mandarin.com.mandarin_backend.controller;

import mandarin.com.mandarin_backend.dto.*;
import mandarin.com.mandarin_backend.entity.Simulation;
import mandarin.com.mandarin_backend.entity.SimulationMessage;
import mandarin.com.mandarin_backend.entity.User;
import mandarin.com.mandarin_backend.entity.UserCharacter;
import mandarin.com.mandarin_backend.entity.enums.SimulationCategory;
import mandarin.com.mandarin_backend.repository.SimulationRepository;
import mandarin.com.mandarin_backend.repository.SimulationMessageRepository;
import mandarin.com.mandarin_backend.repository.UserRepository;
import mandarin.com.mandarin_backend.service.ChatService;
import mandarin.com.mandarin_backend.service.ReportService;
import mandarin.com.mandarin_backend.service.KakaoTalkParseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ReportService reportService;
    private final SimulationRepository simulationRepository;
    private final UserRepository userRepository;
    private final SimulationMessageRepository simulationMessageRepository;
    private final KakaoTalkParseService kakaoTalkParseService;
    
    @Value("${file.upload-dir}")
    private String uploadDir;

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
     * @param request simulationId(시뮬레이션 ID), id(사용자 id)
     * @return 시뮬레이션 분석 보고서
     */
    @PostMapping("/report")
    public ResponseEntity<ReportResponseDto> createReport(@RequestBody ReportRequestDto request) {
        
        // 필수 파라미터 검증
        if (request.getSimulationId() == null) {
            throw new IllegalArgumentException("simulationId는 필수입니다.");
        }
        if (request.getId() == null) {
            throw new IllegalArgumentException("id(사용자 id)는 필수입니다.");
        }

        // 1. Simulation 조회
        Simulation simulation = simulationRepository.findById(request.getSimulationId()).orElseThrow(() -> new IllegalArgumentException("시뮬레이션을 찾을 수 없습니다: " + request.getSimulationId()));

        // 2. User 조회 및 검증증
        User user = userRepository.findById(request.getId()).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + request.getId()));

        // 시뮬레이션의 사용자와 요청한 사용자 ID가 일치하는지 검증
        if (!simulation.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("시뮬레이션의 사용자와 요청한 사용자 ID가 일치하지 않습니다.");
        }

        // 3. UserCharacter 조회
        UserCharacter character = simulation.getCharacter();
        if (character == null) {
            throw new IllegalArgumentException("시뮬레이견에 연결된 캐릭터를 찾을 수 없습니다.");
        }

        // 4. SimulaitonMessage에서 chat_logs 가져오기
        List<SimulationMessage> messages = simulationMessageRepository.findBySimulationSimulationIdOrderByTimestampAsc(request.getSimulationId());

        if (messages.isEmpty()) {
            throw new IllegalArgumentException("시뮬레이션에 대화 로그가 없습니다.");
        }

        List<ChatLogDto> chatLogs = messages.stream()
            .map(msg -> ChatLogDto.builder()
                .role(msg.getSender()) // "user" 또는 "assistant"
                .content(msg.getContent())
                .build())
            .collect(Collectors.toList());

        // 5. user_name 가져오기 (kakaoName)
        String kakaoName = character.getKakaoName();
        if (kakaoName == null || kakaoName.trim().isEmpty()) {
            throw new IllegalArgumentException("카카오톡 이름이 없습니다.");
        }
        String userName = kakaoName;

        // 6. target_name 가져오기 (fullDialogue의 participants에서 kakaoName 제외한 상대방)
        String targetName = findTargetNameFromParticipants(character, kakaoName);
        if (targetName == null || targetName.trim().isEmpty()) {
            targetName = character.getCharacterName(); // kakaoName이 없으면 characterName 사용
            System.out.println("[Report] participants에서 상대방을 찾지 못해 characterName 사용: " + targetName);
        }

        // 7. scenario_type 가져오기 (Simulation.category 기반)
        String scenarioType = determineScenarioType(simulation.getCategory());

        System.out.println("[Report] 보고서 요청 - 시뮬레이션ID: " + request.getSimulationId()
            + ", 사용자: " + userName 
            + ", 대상: " + targetName
            + ", 시나리오: " + scenarioType);
        
        ReportResponseDto response = reportService.createReportAndSave(
            request.getSimulationId(),
            chatLogs,
            userName,
            targetName,
            scenarioType
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * fullDialogue에서 participants를 추출하여 kakaoName을 제외한 상대방 찾기
     */
    private String findTargetNameFromParticipants(UserCharacter character, String kakaoName) {
        try {
            String fullDialogue = character.getFullDialogue();
            if (fullDialogue == null || fullDialogue.trim().isEmpty()) {
                return null;
            }

            // fullDialogue가 JSON 문자열인지 파일 경로인지 확인
            String dialogueJson;
            if (fullDialogue.trim().startsWith("{")) {
                // JSON 문자열인 경우
                dialogueJson = fullDialogue;
            } else {
                // 파일 경로인 경우 파일 읽기
                try {
                    Path filePath = Paths.get(uploadDir, fullDialogue);
                    if (!Files.exists(filePath)) {
                        System.out.println("[Report] 대화 파일이 존재하지 않습니다: " + filePath.toAbsolutePath());
                        return null;
                    }
                    dialogueJson = Files.readString(filePath, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    System.out.println("[Report] 대화 파일 읽기 실패: " + e.getMessage());
                    return null;
                }
            }

            // JSON에서 participants 추출
            ParsedDialogueDto dialogueDto = kakaoTalkParseService.parseJsonToDto(dialogueJson);
            List<String> participants = dialogueDto.getParticipants();

            // participants에서 kakaoName 제외한 상대방 찾기
            return participants.stream()
                    .filter(name -> !name.equals(kakaoName))
                    .findFirst()
                    .orElse(null);
            } catch (Exception e) {
                System.out.println("[Report] participants 추출 실패: " + e.getMessage());
                return null;
        }
    }
    
    /**
     * SimulationCategory를 기반으로 scenario_type 결정
     * PAST: EMOTIONAL_MISTAKE, MISCOMMUNICATION, CONTACT_ISSUE, BREAKUP_PROCESS, REALITY_PROBLEM
     * FUTURE: RELATION_TENSION, PERSONAL_BOUNDARY, FAMILY_FRIEND_ISSUE, BREAKUP_FUTURE, EVENT_PREPARATION
     */
    private String determineScenarioType(SimulationCategory category) {
        return switch (category) {
            case EMOTIONAL_MISTAKE, MISCOMMUNICATION, CONTACT_ISSUE, 
                 BREAKUP_PROCESS, REALITY_PROBLEM -> "PAST";
            case RELATION_TENSION, PERSONAL_BOUNDARY, FAMILY_FRIEND_ISSUE, 
                 BREAKUP_FUTURE, EVENT_PREPARATION -> "FUTURE";
        };
    }
}

