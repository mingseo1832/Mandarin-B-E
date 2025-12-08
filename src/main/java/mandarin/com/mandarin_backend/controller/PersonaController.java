package mandarin.com.mandarin_backend.controller;

import mandarin.com.mandarin_backend.dto.AnalyzeRequestDto;
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

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/persona")
@RequiredArgsConstructor
public class PersonaController {

    private final AnalysisService analysisService;
    private final KakaoTalkParseService kakaoTalkParseService;
    private final UserCharacterRepository userCharacterRepository;
    private final UserRepository userRepository;

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
            @RequestParam("targetName") String targetName,
            @RequestParam("characterName") String characterName,
            @RequestParam(value = "characterAge", defaultValue = "0") int characterAge,
            @RequestParam(value = "relationType", defaultValue = "0") int relationType) {
        
        System.out.println("[Upload] 파일 업로드 - 파일명: " + file.getOriginalFilename() 
            + ", 사용자: " + userId + ", 대상: " + targetName);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        try {
            // 1. 파일 내용 읽기
            String rawTextContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            // 2. Java 파서로 파싱 및 통계 조회 (대상 인물 확인용)
            ParsedChatDataDto parsedData = kakaoTalkParseService.parseInfo(rawTextContent);
            
            // 3. 대상 인물 존재 여부 확인
            if (!parsedData.getParticipants().contains(targetName)) {
                throw new IllegalArgumentException(
                    "대상 인물 '" + targetName + "'을(를) 찾을 수 없습니다. " +
                    "참여자 목록: " + parsedData.getParticipants()
                );
            }

            // 4. 파싱 + PII 마스킹 + JSON 변환 (최초 1회만 파싱)
            String dialogueJson = kakaoTalkParseService.parseAndConvertToJson(rawTextContent);

            // 5. 사용자 조회
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

            // 6. 기존 캐릭터 확인 또는 새로 생성
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

            // 7. DB 저장
            UserCharacter savedCharacter = userCharacterRepository.save(character);

            // 8. 응답 반환
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
}