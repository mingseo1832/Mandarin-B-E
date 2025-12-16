package mandarin.com.mandarin_backend.controller;

import mandarin.com.mandarin_backend.dto.AnalyzeRequestDto;
import mandarin.com.mandarin_backend.dto.HistorySumRequestDto;
import mandarin.com.mandarin_backend.dto.ParseInfoResponseDto;
import mandarin.com.mandarin_backend.dto.ParsedChatDataDto;
import mandarin.com.mandarin_backend.entity.User;
import mandarin.com.mandarin_backend.entity.UserCharacter;
import mandarin.com.mandarin_backend.entity.enums.SimulationCategory;
import mandarin.com.mandarin_backend.entity.enums.SimulationPurpose;
import mandarin.com.mandarin_backend.repository.UserCharacterRepository;
import mandarin.com.mandarin_backend.repository.UserRepository;
import mandarin.com.mandarin_backend.service.AnalysisService;
import mandarin.com.mandarin_backend.service.KakaoTalkParseService;
import mandarin.com.mandarin_backend.service.UserCharacterService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/persona")
@RequiredArgsConstructor
public class PersonaController {

    private final AnalysisService analysisService;
    private final KakaoTalkParseService kakaoTalkParseService;
    private final UserCharacterRepository userCharacterRepository;
    private final UserRepository userRepository;
    private final UserCharacterService userCharacterService;

    /**
     * 페르소나 추출 및 시뮬레이션 생성
     * POST /api/persona/analyze
     * 
     * DB의 fullDialogue에서 특정 날짜 기준으로 데이터를 필터링하여
     * 상대방(kakaoName 제외) 페르소나 추출 후 Simulation에 저장
     * 
     * @param request characterId, targetDate, bufferDays, simulationName, purpose, category
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyze(@RequestBody AnalyzeRequestDto request) {

        // 필수 파라미터 검증
        if (request.getUserId() == null || request.getUserId().isEmpty()) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        if (request.getCharacterId() == null) {
            throw new IllegalArgumentException("characterId는 필수입니다.");
        }
        if (request.getSimulationName() == null || request.getSimulationName().isEmpty()) {
            throw new IllegalArgumentException("simulationName은 필수입니다.");
        }
        if (request.getPurpose() == null || request.getPurpose().isEmpty()) {
            throw new IllegalArgumentException("purpose는 필수입니다.");
        }
        if (request.getCategory() == null || request.getCategory().isEmpty()) {
            throw new IllegalArgumentException("category는 필수입니다.");
        }

        // targetDate 파싱 (없으면 null로 전달 -> 가장 최신 날짜 사용)
        LocalDate targetDate = null;
        if (request.getTargetDate() != null && !request.getTargetDate().isEmpty()) {
            targetDate = LocalDate.parse(request.getTargetDate());
        }

        // purpose, category 파싱
        SimulationPurpose purpose;
        SimulationCategory category;
        try {
            purpose = SimulationPurpose.valueOf(request.getPurpose());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 purpose입니다: " + request.getPurpose());
        }
        try {
            category = SimulationCategory.valueOf(request.getCategory());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 category입니다: " + request.getCategory());
        }
        
        System.out.println("[Analyze] 시뮬레이션 생성 요청 - 캐릭터ID: " + request.getCharacterId()
            + ", 기준날짜: " + (request.getTargetDate() != null ? request.getTargetDate() : "최신")
            + ", 버퍼: " + request.getBufferDays() + "일"
            + ", 시뮬레이션명: " + request.getSimulationName()
            + ", 목적: " + purpose + ", 카테고리: " + category);

        // 페르소나 추출 및 시뮬레이션 생성
        AnalysisService.AnalysisResult result = analysisService.analyzeAndCreateSimulation(
            request.getUserId(),
            request.getCharacterId(),
            targetDate,
            request.getBufferDays(),
            request.getSimulationName(),
            purpose,
            category
        );

        // 응답 반환
        return ResponseEntity.ok(Map.of(
            "success", true,
            "simulationId", result.getSimulation().getSimulationId(),
            "simulationName", result.getSimulation().getSimulationName(),
            "targetName", result.getTargetName(),
            "persona", result.getPersona(),
            "fewShotContextLength", result.getFewShotContextLength(),
            "targetMessageCount", result.getTargetMessageCount()
        ));
    }

    /**
     * 카카오톡 텍스트 파일 파싱 정보 조회 (Java 파서 사용 - Python 호출 X)
     * POST /api/persona/parse-info
     * 
     * @param file 카카오톡 대화 내보내기 텍스트 파일 (.txt)
     * @return 파싱 정보 (참여자 목록, 대화 기간, 메시지 수 등)
     */
    @PostMapping("/parse-info")
    public ResponseEntity<ParseInfoResponseDto> parseInfo(@RequestParam("file") MultipartFile file) {
        
        System.out.println("[ParseInfo] 파일 업로드 - 파일명: " + file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        try {
            // Java 파서로 직접 파싱 (Python 호출 없음)
            ParsedChatDataDto parsedData = kakaoTalkParseService.parseInfo(file);
            
            // ParseInfoResponseDto로 변환
            ParseInfoResponseDto result = ParseInfoResponseDto.builder()
                    .formatType(parsedData.getFormatType())
                    .totalDays(parsedData.getTotalDays())
                    .totalMessages(parsedData.getTotalMessages())
                    .participants(parsedData.getParticipants())
                    .participantCount(parsedData.getParticipantCount())
                    .dateRange(Map.of(
                        "start", parsedData.getStartDate() != null ? parsedData.getStartDate().toString() : "",
                        "end", parsedData.getEndDate() != null ? parsedData.getEndDate().toString() : ""
                    ))
                    .build();
            
            return ResponseEntity.ok(result);
            
        } catch (IOException e) {
            throw new RuntimeException("파일 읽기 실패: " + e.getMessage());
        }
    }

    /**
     * 카카오톡 대화 파일 업로드 및 DB 저장
     * POST /api/persona/upload
     * 
     * 파일을 파싱하고 PII 마스킹 후 JSON 형태로 UserCharacter 테이블의 fullDialogue에 저장
     * (이후 사용 시 재파싱 불필요)
     * 
     * @param file 카카오톡 대화 내보내기 텍스트 파일 (.txt)
     * @param userId 사용자 ID (DB의 userId 문자열)
     * @param targetName 분석 대상 인물 이름 (카카오톡에서의 이름)
     * @param characterName 캐릭터 이름 (사용자가 지정)
     * @param characterAge 캐릭터 나이
     * @param relationType 관계 타입 코드값
     * @return 저장된 캐릭터 정보
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadAndSave(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam("kakaoName") String kakaoName,
            @RequestParam("characterName") String characterName,
            @RequestParam(value = "characterAge", defaultValue = "0") int characterAge,
            @RequestParam(value = "relationType", defaultValue = "0") int relationType) {
        
        System.out.println("[Upload] 파일 업로드 - 파일명: " + file.getOriginalFilename() 
            + ", 사용자: " + userId + ", 대상: " + kakaoName);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        try {
            // 1. 파일 내용 읽기
            String rawTextContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            // 2. Java 파서로 파싱 및 통계 조회 (대상 인물 확인용)
            ParsedChatDataDto parsedData = kakaoTalkParseService.parseInfo(rawTextContent);
            
            // 3. 대상 인물 존재 여부 확인
            if (!parsedData.getParticipants().contains(kakaoName)) {
                throw new IllegalArgumentException(
                    "대화에서 '" + kakaoName + "'을(를) 찾을 수 없습니다. " +
                    "참여자 목록: " + parsedData.getParticipants()
                );
            }

            // 4. participants에서 상대방 찾기(kakaoName 제외)
            List<String> participants = parsedData.getParticipants();
            String targetName = participants.stream()
                .filter(name -> !name.equals(kakaoName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "1:1 대화에서 상대방을 찾을 수 없습니다. 참여자: " + participants));

            System.out.println("[Upload] 상대방 자동 찾기 - 본인: " + kakaoName + ", 상대방: " + targetName);

            // 5. 파싱 + PII 마스킹 + JSON 변환 (최초 1회만 파싱)
            String dialogueJson = kakaoTalkParseService.parseAndConvertToJson(rawTextContent);

            // 6. 사용자 조회
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

            // 7. 기존 캐릭터 확인 또는 새로 생성
            UserCharacter character = userCharacterRepository
                    .findByUserAndKakaoName(user, targetName)
                    .orElse(null);

            if (character == null) {
                // 새 캐릭터 생성
                character = UserCharacter.builder()
                        .user(user)
                        .characterName(characterName)
                        .characterAge(characterAge)
                        .relationType(relationType)
                        .kakaoName(targetName)
                        .fullDialogue(dialogueJson)
                        .build();
            } else {
                // 기존 캐릭터 업데이트
                character.setCharacterName(characterName);
                character.setCharacterAge(characterAge);
                character.setRelationType(relationType);
                character.setFullDialogue(dialogueJson);
            }

            // 8. DB 저장
            UserCharacter savedCharacter = userCharacterRepository.save(character);

            // 9. 응답 반환
            return ResponseEntity.ok(Map.of(
                "success", true,
                "characterId", savedCharacter.getCharacterId(),
                "characterName", savedCharacter.getCharacterName(),
                "kakaoName", savedCharacter.getKakaoName(),
                "dialogueJsonLength", dialogueJson.length(),
                "participants", parsedData.getParticipants(),
                "totalMessages", parsedData.getTotalMessages(),
                "dateRange", Map.of(
                    "start", parsedData.getStartDate() != null ? parsedData.getStartDate().toString() : "",
                    "end", parsedData.getEndDate() != null ? parsedData.getEndDate().toString() : ""
                )
            ));
            
        } catch (IOException e) {
            throw new RuntimeException("파일 읽기 실패: " + e.getMessage());
        }
    }

    /**
     * 카카오톡 파일 업로드 및 마스킹 처리 (DB 저장 없이 JSON 반환)
     * POST /api/persona/upload-and-mask
     * 
     * 파일을 파싱하고 PII 마스킹 후 JSON 문자열로 반환
     * 프론트엔드에서 이 JSON을 받아서 엔티티 생성 API에 전달
     * 
     * @param file 카카오톡 대화 내보내기 텍스트 파일 (.txt)
     * @return 마스킹된 JSON 문자열
     */
    @PostMapping("/upload-and-mask")
    public ResponseEntity<Map<String, Object>> uploadAndMask(
            @RequestParam("file") MultipartFile file) {
        
        System.out.println("[UploadAndMask] 파일 업로드 - 파일명: " + file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        try {
            // 1. 파일 내용 읽기
            String rawTextContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            // 2. 파싱 + PII 마스킹 + JSON 변환
            String dialogueJson = kakaoTalkParseService.parseAndConvertToJson(rawTextContent);

            // 3. JSON 응답 반환
            return ResponseEntity.ok(Map.of(
                "success", true,
                "dialogueJson", dialogueJson  // 마스킹된 JSON 문자열
            ));
            
        } catch (IOException e) {
            throw new RuntimeException("파일 읽기 실패: " + e.getMessage());
        }
    }

    /**
     * 히스토리 요약 (DB 저장 없이 프론트엔드로 반환만)
     * POST /api/persona/history-summary
     * 
     * 사용자가 입력한 히스토리를 AI가 요약하여 프론트엔드로 반환
     * 클라이언트가 캐릭터 생성 시 이 값을 historySum으로 전달
     * 
     * @param request characterName, history
     * @return 요약된 히스토리
     */
    @PostMapping("/history-summary")
    public ResponseEntity<Map<String, Object>> summarizeHistory(@RequestBody HistorySumRequestDto request) {
        
        // 필수 파라미터 검증
        if (request.getCharacterName() == null || request.getCharacterName().trim().isEmpty()) {
            throw new IllegalArgumentException("characterName은 필수입니다.");
        }
        if (request.getHistory() == null || request.getHistory().trim().isEmpty()) {
            throw new IllegalArgumentException("history는 필수입니다.");
        }
        
        System.out.println("[HistorySummary] 히스토리 요약 요청 - 캐릭터이름: " + request.getCharacterName()
            + ", 히스토리 길이: " + request.getHistory().length() + "자");
        
        // 히스토리 요약만 수행 (DB 저장 없음)
        String summary = userCharacterService.summarizeHistory(
            request.getHistory(),
            request.getCharacterName()
        );
        
        // 응답 반환 (DB 저장 없이 요약 결과만 반환)
        return ResponseEntity.ok(Map.of(
            "success", true,
            "historySum", summary
        ));
    }
}