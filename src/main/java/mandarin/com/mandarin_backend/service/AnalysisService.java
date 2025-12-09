package mandarin.com.mandarin_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mandarin.com.mandarin_backend.dto.ParseInfoResponseDto;
import mandarin.com.mandarin_backend.dto.ParsedChatDataDto;
import mandarin.com.mandarin_backend.dto.ParsedDialogueDto;
import mandarin.com.mandarin_backend.dto.UserPersonaDto;
import mandarin.com.mandarin_backend.entity.Simulation;
import mandarin.com.mandarin_backend.entity.UserCharacter;
import mandarin.com.mandarin_backend.entity.enums.SimulationCategory;
import mandarin.com.mandarin_backend.entity.enums.SimulationPurpose;
import mandarin.com.mandarin_backend.repository.SimulationRepository;
import mandarin.com.mandarin_backend.repository.UserCharacterRepository;
import mandarin.com.mandarin_backend.util.KakaoTalkParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final WebClient webClient;
    private final UserCharacterRepository userCharacterRepository;
    private final SimulationRepository simulationRepository;
    private final KakaoTalkParseService kakaoTalkParseService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 페르소나 추출 및 시뮬레이션 생성
     * 
     * 1. DB에서 fullDialogue(JSON) 조회
     * 2. kakaoName 제외한 상대방 자동 찾기 (1:1 채팅)
     * 3. 날짜 기준 필터링
     * 4. 상대방 페르소나 추출
     * 5. Simulation 생성 및 저장 (fewShotContext, characterPersona)
     * 
     * @param characterId DB에서 fullDialogue를 조회할 캐릭터 ID
     * @param targetDate 기준 날짜 (null이면 가장 최신 날짜)
     * @param bufferDays 기준 날짜 이전 버퍼 일수 (기본 7일)
     * @param simulationName 시뮬레이션 이름
     * @param purpose 시뮬레이션 목적 (FUTURE/PAST)
     * @param category 시뮬레이션 카테고리
     * @return 생성된 시뮬레이션 정보
     */
    public AnalysisResult analyzeAndCreateSimulation(
            Long characterId, 
            LocalDate targetDate, 
            Integer bufferDays,
            String simulationName,
            SimulationPurpose purpose,
            SimulationCategory category) {
        
        // 1. DB에서 캐릭터 및 fullDialogue(JSON) 조회
        UserCharacter character = userCharacterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("캐릭터를 찾을 수 없습니다: " + characterId));
        
        String dialogueJson = character.getFullDialogue();
        if (dialogueJson == null || dialogueJson.isEmpty()) {
            throw new IllegalArgumentException("저장된 대화 내용이 없습니다.");
        }

        // 2. JSON에서 참여자 목록 조회하여 상대방 찾기 (kakaoName 제외)
        ParsedDialogueDto dialogueDto = kakaoTalkParseService.parseJsonToDto(dialogueJson);
        List<String> participants = dialogueDto.getParticipants();
        
        String kakaoName = character.getKakaoName(); // 사용자 본인
        String targetName = participants.stream()
                .filter(name -> !name.equals(kakaoName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "1:1 대화에서 상대방을 찾을 수 없습니다. 참여자: " + participants));

        // 3. JSON에서 직접 날짜 기준 필터링 (재파싱 없음)
        int effectiveBufferDays = bufferDays != null ? bufferDays : 7;
        KakaoTalkParseService.PreprocessResult preprocessed = 
            kakaoTalkParseService.filterFromJson(
                dialogueJson, targetName, targetDate, effectiveBufferDays, 50000);
        
        if (!preprocessed.isTargetFound()) {
            throw new IllegalArgumentException(
                "대상 인물 '" + targetName + "'의 메시지를 찾을 수 없습니다.");
        }

        System.out.println("[Analyze] 시뮬레이션 생성 - 캐릭터ID: " + characterId 
            + ", 사용자(kakaoName): " + kakaoName
            + ", 상대방: " + targetName
            + ", 기준날짜: " + (targetDate != null ? targetDate : "최신")
            + ", 버퍼: " + effectiveBufferDays + "일"
            + ", 상대방메시지수: " + preprocessed.getTargetMessageCount());

        // 4. Python 서버로 AI 분석 요청 (상대방 페르소나 추출)
        UserPersonaDto persona = analyzePersonaWithText(preprocessed.getText(), targetName);

        // 5. Simulation 생성 및 저장
        String personaJson;
        try {
            personaJson = objectMapper.writeValueAsString(persona);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("페르소나 JSON 변환 실패: " + e.getMessage(), e);
        }

        Simulation simulation = Simulation.builder()
                .character(character)
                .simulationName(simulationName)
                .purpose(purpose)
                .category(category)
                .fewShotContext(preprocessed.getText())  // 필터링된 대화 데이터
                .characterPersona(personaJson)           // 추출된 페르소나 (JSON)
                .build();

        Simulation savedSimulation = simulationRepository.save(simulation);

        System.out.println("[Analyze] 시뮬레이션 저장 완료 - ID: " + savedSimulation.getSimulationId());

        return AnalysisResult.builder()
                .simulation(savedSimulation)
                .persona(persona)
                .targetName(targetName)
                .fewShotContextLength(preprocessed.getText().length())
                .targetMessageCount(preprocessed.getTargetMessageCount())
                .build();
    }

    /**
     * 분석 결과 클래스
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AnalysisResult {
        private Simulation simulation;
        private UserPersonaDto persona;
        private String targetName;
        private int fewShotContextLength;
        private int targetMessageCount;
    }

    /**
     * 이미 필터링된 텍스트로 페르소나 추출 (Python AI 분석)
     * 
     * @param filteredText 필터링된 대화 텍스트
     * @param targetName 분석 대상 인물 이름
     * @return 추출된 페르소나 정보
     */
    public UserPersonaDto analyzePersonaWithText(String filteredText, String targetName) {
        // Python 서버로 보낼 데이터 준비 (이미 필터링된 텍스트)
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("text_content", filteredText);
        requestBody.put("target_name", targetName);
        // Python에서 추가 필터링 하지 않도록 전체 기간 설정
        requestBody.put("period_days", 9999);
        requestBody.put("buffer_days", 0);

        // Python 서버 호출 (POST /analyze) - AI 분석만 Python에서 수행
        UserPersonaDto response = webClient.post()
                .uri("/analyze")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(UserPersonaDto.class)
                .block();
        
        return response;
    }

    /**
     * 카카오톡 대화 파일의 파싱 정보 조회 (Java 파서 사용)
     * (참여자 목록, 대화 기간, 메시지 수 등)
     * 
     * @param textContent 카카오톡 대화 텍스트
     * @return 파싱 정보
     */
    public ParseInfoResponseDto parseInfo(String textContent) {
        // Java 파서로 직접 파싱 (Python 호출 X)
        KakaoTalkParser parser = new KakaoTalkParser(textContent);
        ParsedChatDataDto stats = parser.getStatistics();
        
        // ParseInfoResponseDto로 변환
        return ParseInfoResponseDto.builder()
                .formatType(stats.getFormatType())
                .totalDays(stats.getTotalDays())
                .totalMessages(stats.getTotalMessages())
                .participants(stats.getParticipants())
                .participantCount(stats.getParticipantCount())
                .dateRange(Map.of(
                    "start", stats.getStartDate() != null ? stats.getStartDate().toString() : null,
                    "end", stats.getEndDate() != null ? stats.getEndDate().toString() : null
                ))
                .build();
    }

    /**
     * 카카오톡 대화를 파싱하여 기간 필터링된 텍스트 반환 (Java 파서 사용)
     * 
     * @param textContent 원본 대화 텍스트
     * @param periodDays 필터링할 일수 (startDate/endDate 미지정 시 사용)
     * @param startDate 시작일
     * @param endDate 종료일
     * @param bufferDays 시작일 이전 버퍼 일수
     * @return 필터링된 대화 텍스트
     */
    public String preprocessText(String textContent, Integer periodDays, 
            LocalDate startDate, LocalDate endDate, Integer bufferDays) {
        
        KakaoTalkParser parser = new KakaoTalkParser(textContent);
        
        var filteredData = parser.filterByPeriod(
            periodDays != null ? periodDays : 14,
            null,
            startDate,
            endDate,
            bufferDays != null ? bufferDays : 7
        );
        
        return parser.toText(filteredData);
    }
}