package mandarin.com.mandarin_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mandarin.com.mandarin_backend.dto.ParseInfoResponseDto;
import mandarin.com.mandarin_backend.dto.ParsedChatDataDto;
import mandarin.com.mandarin_backend.dto.ParsedDialogueDto;
import mandarin.com.mandarin_backend.dto.ReactionTriggerDto;
import mandarin.com.mandarin_backend.dto.UserPersonaDto;
import mandarin.com.mandarin_backend.entity.ReportCharacter;
import mandarin.com.mandarin_backend.entity.ReportCharacterDetailLog;
import mandarin.com.mandarin_backend.entity.Simulation;
import mandarin.com.mandarin_backend.entity.User;
import mandarin.com.mandarin_backend.entity.UserCharacter;
import mandarin.com.mandarin_backend.entity.enums.SimulationCategory;
import mandarin.com.mandarin_backend.entity.enums.SimulationPurpose;
import mandarin.com.mandarin_backend.repository.ReportCharacterDetailLogRepository;
import mandarin.com.mandarin_backend.repository.ReportCharacterRepository;
import mandarin.com.mandarin_backend.repository.SimulationRepository;
import mandarin.com.mandarin_backend.repository.UserCharacterRepository;
import mandarin.com.mandarin_backend.repository.UserRepository;
import mandarin.com.mandarin_backend.util.KakaoTalkParser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final WebClient webClient;
    private final UserCharacterRepository userCharacterRepository;
    private final UserRepository userRepository;
    private final SimulationRepository simulationRepository;
    private final ReportCharacterRepository reportCharacterRepository;
    private final ReportCharacterDetailLogRepository reportCharacterDetailLogRepository;
    private final KakaoTalkParseService kakaoTalkParseService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * 페르소나 추출 및 시뮬레이션 생성
     * 
     * 1. DB에서 fullDialogue(JSON) 조회
     * 2. kakaoName 제외한 상대방 자동 찾기 (1:1 채팅)
     * 3. 날짜 기준 필터링
     * 4. 상대방 페르소나 추출
     * 5. Simulation 생성 및 저장 (fewShotContext, characterPersona)
     * 6. ReportCharacter 및 ReportCharacterDetailLog 저장 (negative_triggers 기반)
     * 
     * @param characterId DB에서 fullDialogue를 조회할 캐릭터 ID
     * @param targetDate 기준 날짜 (null이면 가장 최신 날짜)
     * @param bufferDays 기준 날짜 이전 버퍼 일수 (기본 7일)
     * @param simulationName 시뮬레이션 이름
     * @param purpose 시뮬레이션 목적 (FUTURE/PAST)
     * @param category 시뮬레이션 카테고리
     * @return 생성된 시뮬레이션 정보
     */
    @Transactional
    public AnalysisResult analyzeAndCreateSimulation(
            Long id,
            Long characterId, 
            LocalDate targetDate, 
            Integer bufferDays,
            String simulationName,
            SimulationPurpose purpose,
            SimulationCategory category) {
        
        // 1. DB에서 캐릭터 및 fullDialogue(파일 경로) 조회
        UserCharacter character = userCharacterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("캐릭터를 찾을 수 없습니다: " + characterId));
        
        String dialoguePath = character.getFullDialogue();
        if (dialoguePath == null || dialoguePath.isEmpty()) {
            throw new IllegalArgumentException("저장된 대화 파일 경로가 없습니다.");
        }

        // 2. 파일 경로에서 대화 JSON 파일 읽기
        String dialogueJson = readDialogueFromFile(dialoguePath);
        if (dialogueJson == null || dialogueJson.isEmpty()) {
            throw new IllegalArgumentException("대화 파일을 읽을 수 없습니다: " + dialoguePath);
        }

        // 3. JSON에서 참여자 목록 조회하여 상대방 찾기 (kakaoName 제외)
        ParsedDialogueDto dialogueDto = kakaoTalkParseService.parseJsonToDto(dialogueJson);
        List<String> participants = dialogueDto.getParticipants();
        
        String kakaoName = character.getKakaoName(); // 사용자 본인
        String targetName = participants.stream()
                .filter(name -> !name.equals(kakaoName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "1:1 대화에서 상대방을 찾을 수 없습니다. 참여자: " + participants));

        // 4. JSON에서 직접 날짜 기준 필터링 (재파싱 없음)
        int effectiveBufferDays = bufferDays != null ? bufferDays : 7;
        KakaoTalkParseService.PreprocessResult preprocessed = 
            kakaoTalkParseService.filterFromJson(
                dialogueJson, targetName, targetDate, effectiveBufferDays, 
                KakaoTalkParseService.getDefaultMaxChars());
        
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

        // 5. Python 서버로 AI 분석 요청 (상대방 페르소나 추출)
        UserPersonaDto persona = analyzePersonaWithText(preprocessed.getText(), targetName);

        // 6. Simulation 생성 및 저장
        String personaJson;
        try {
            personaJson = objectMapper.writeValueAsString(persona);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("페르소나 JSON 변환 실패: " + e.getMessage(), e);
        }

        User user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + id));

        Simulation simulation = Simulation.builder()
                .user(user)
                .character(character)
                .simulationName(simulationName)
                .purpose(purpose)
                .category(category)
                .fewShotContext(preprocessed.getText())  // 필터링된 대화 데이터
                .characterPersona(personaJson)           // 추출된 페르소나 (JSON)
                .build();

        Simulation savedSimulation = simulationRepository.save(simulation);

        System.out.println("[Analyze] 시뮬레이션 저장 완료 - ID: " + savedSimulation.getSimulationId());

        // 6. ReportCharacter 생성 로직 제거 (UserCharacter 생성 시점으로 이동)

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
     * fullDialogue에 저장된 파일 경로에서 대화 JSON 파일 읽기
     * 
     * @param dialoguePath DB에 저장된 파일 경로 (uploads/ 이후 경로)
     * @return 파일에서 읽은 대화 JSON 문자열
     */
    private String readDialogueFromFile(String dialoguePath) {
        try {
            // uploadDir과 dialoguePath를 조합하여 전체 경로 생성
            Path filePath = Paths.get(uploadDir, dialoguePath);
            
            System.out.println("[AnalysisService] 대화 파일 읽기 - 경로: " + filePath.toAbsolutePath());
            
            // 파일 존재 여부 확인
            if (!Files.exists(filePath)) {
                throw new IllegalArgumentException("대화 파일이 존재하지 않습니다: " + filePath.toAbsolutePath());
            }
            
            // 파일 내용 읽기 (UTF-8 인코딩)
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            
            System.out.println("[AnalysisService] 대화 파일 읽기 완료 - 파일 크기: " + content.length() + "자");
            
            return content;
        } catch (IOException e) {
            throw new RuntimeException("대화 파일 읽기 실패: " + dialoguePath + ", 오류: " + e.getMessage(), e);
        }
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

    /**
     * negative_triggers를 기반으로 ReportCharacter 및 ReportCharacterDetailLog 저장
     * 
     * @param character 대상 캐릭터
     * @param negativeTriggers 부정적 반응 트리거 목록 (최대 3개)
     * @param userName 사용자 이름 (카카오톡 본인)
     * @param targetName 상대방 이름 (분석 대상)
     */
    private void saveReportCharacterFromNegativeTriggers(
            UserCharacter character,
            List<ReactionTriggerDto> negativeTriggers,
            String userName,
            String targetName) {
        
        // 기존 ReportCharacter 삭제 (새로 분석된 결과로 교체)
        List<ReportCharacter> existingReports = reportCharacterRepository.findByCharacter_CharacterId(character.getCharacterId());
        for (ReportCharacter existing : existingReports) {
            reportCharacterDetailLogRepository.deleteByReportCharacter_ReportCharacterId(existing.getReportCharacterId());
        }
        reportCharacterRepository.deleteAll(existingReports);
        
        LocalDateTime baseTime = LocalDateTime.now();
        int messageOrder = 0;  // 전체 메시지 순서 (모든 ReportCharacter의 DetailLog에 걸쳐 순차 증가)
        
        // 위험도 배열 (첫 번째가 가장 심각: 90, 70, 50)
        int[] dangerLevels = {90, 70, 50};
        
        for (int i = 0; i < negativeTriggers.size(); i++) {
            ReactionTriggerDto trigger = negativeTriggers.get(i);
            int dangerLevel = i < dangerLevels.length ? dangerLevels[i] : 40;
            
            // 1. ReportCharacter 저장
            ReportCharacter reportCharacter = ReportCharacter.builder()
                    .character(character)
                    .conflictName(trigger.getTrigger())  // 키워드
                    .dangerLevel(dangerLevel)
                    .description(trigger.getReaction())  // 부정적 반응 패턴 설명
                    .solution(generateSolution(trigger.getTrigger(), trigger.getReaction()))
                    .build();
            
            ReportCharacter savedReport = reportCharacterRepository.save(reportCharacter);
            
            // 2. ReportCharacterDetailLog 저장 (example 문장 파싱)
            if (trigger.getExample() != null && !trigger.getExample().isEmpty()) {
                messageOrder = saveExampleAsDetailLogs(
                    savedReport, 
                    trigger.getExample(), 
                    userName, 
                    targetName, 
                    baseTime, 
                    messageOrder
                );
            }
        }
        
        System.out.println("[Analyze] ReportCharacter 저장 완료 - 캐릭터ID: " + character.getCharacterId() 
            + ", 갈등요소 개수: " + negativeTriggers.size());
    }

    /**
     * 갈등 요소에 대한 해결 방안 생성
     * 
     * @param conflictName 갈등 요소 이름
     * @param reaction 부정적 반응 패턴
     * @return 관계 개선 조언
     */
    private String generateSolution(String conflictName, String reaction) {
        // 기본 템플릿 기반 조언 생성
        StringBuilder solution = new StringBuilder();
        solution.append("'").append(conflictName).append("'에 대해 상대방이 민감하게 반응하는 것으로 보입니다. ");
        solution.append("이 주제에 대해 대화할 때는 상대방의 감정을 먼저 인정하고, ");
        solution.append("공감하는 표현을 사용해보세요. ");
        solution.append("필요하다면 솔직하게 서로의 생각을 나누는 시간을 가져보는 것도 좋습니다.");
        return solution.toString();
    }

    /**
     * example 문장을 파싱하여 ReportCharacterDetailLog로 저장
     * 대화 형식의 예시를 개별 메시지로 분리하여 저장
     * 
     * @param reportCharacter 연결할 ReportCharacter
     * @param example 예시 대화 문자열
     * @param userName 사용자 이름
     * @param targetName 상대방 이름
     * @param baseTime 기준 시간
     * @param startOrder 시작 순서 번호
     * @return 다음 순서 번호
     */
    private int saveExampleAsDetailLogs(
            ReportCharacter reportCharacter,
            String example,
            String userName,
            String targetName,
            LocalDateTime baseTime,
            int startOrder) {
        
        int order = startOrder;
        
        // example 문장을 줄바꿈이나 구분자로 분리 시도
        // 형식 예: "사용자: 메시지1\n상대방: 메시지2" 또는 단순 문장
        String[] lines = example.split("\\n|\\r\\n");
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;
            
            // sender 결정: 이름이 포함되어 있으면 해당 사람, 아니면 문맥 기반 추정
            String sender = determineSender(trimmedLine, userName, targetName);
            String message = extractMessage(trimmedLine, userName, targetName);

            ReportCharacterDetailLog detailLog = ReportCharacterDetailLog.builder()
                    .reportCharacter(reportCharacter)
                    .sender(sender)  // "user" 또는 "character"
                    .messageKakao(message)
                    .timestamp(baseTime.plusSeconds(order))
                    .build();
            
            reportCharacterDetailLogRepository.save(detailLog);
            order++;
        }
        
        // 단일 문장인 경우 (줄바꿈 없음)
        if (lines.length <= 1 && order == startOrder) {
            String message = example.trim();
            if (!message.isEmpty()) {
                // 기본적으로 상대방(CHARACTER)의 반응으로 저장
                ReportCharacterDetailLog detailLog = ReportCharacterDetailLog.builder()
                        .reportCharacter(reportCharacter)
                        .sender("character")  // CHARACTER (분석 대상)
                        .messageKakao(message)
                        .timestamp(baseTime.plusSeconds(order))
                        .build();
                
                reportCharacterDetailLogRepository.save(detailLog);
                order++;
            }
        }
        
        return order;
    }

    /**
     * 메시지 발신자 결정
     * 
     * @param line 대화 라인
     * @param userName 사용자 이름
     * @param targetName 상대방 이름
     * @return true = CHARACTER(상대방), false = USER(사용자)
     */
    private String determineSender(String line, String userName, String targetName) {
        // "이름:" 또는 "이름 :" 형식 확인
        if (line.startsWith(userName + ":") || line.startsWith(userName + " :")) {
            return "user";  // USER
        }
        if (line.startsWith(targetName + ":") || line.startsWith(targetName + " :")) {
            return "character";   // CHARACTER
        }
        
        // 이름이 포함된 경우
        if (line.contains(userName)) {
            return "user";  // USER
        }
        if (line.contains(targetName)) {
            return "character";   // CHARACTER
        }
        
        // 기본값: CHARACTER (분석 대상의 반응이므로)
        return "character";
    }

    /**
     * 라인에서 실제 메시지 내용 추출 (이름 접두사 제거)
     * 
     * @param line 대화 라인
     * @param userName 사용자 이름
     * @param targetName 상대방 이름
     * @return 메시지 내용
     */
    private String extractMessage(String line, String userName, String targetName) {
        // "이름:" 또는 "이름 :" 형식 제거
        if (line.startsWith(userName + ":")) {
            return line.substring(userName.length() + 1).trim();
        }
        if (line.startsWith(userName + " :")) {
            return line.substring(userName.length() + 2).trim();
        }
        if (line.startsWith(targetName + ":")) {
            return line.substring(targetName.length() + 1).trim();
        }
        if (line.startsWith(targetName + " :")) {
            return line.substring(targetName.length() + 2).trim();
        }
        
        // 접두사 없으면 원본 반환
        return line;
    }

    /**
     * UserCharacter 생성 시 fullDialogue에서 부정적 반응 트리거 추출 및 ReportCharacter 저장
     * 
     * @param character 저장된 UserCharacter
     * @param kakaoName 사용자 이름 (카카오톡 본인)
     * @param targetName 상대방 이름 (분석 대상)
     */
    @Transactional
    public void createReportCharacterFromFullDialogue(
        UserCharacter character,
        String kakaoName,
        String targetName) {
    
    // 1. DB에서 가져온 건 '파일 경로'입니다. (변수명 변경: dialogueJson -> filePath)
    String filePath = character.getFullDialogue();
    
    if (filePath == null || filePath.isEmpty()) {
        System.out.println("[ReportCharacter] 대화 파일 경로가 없어 리포트를 생성하지 않습니다.");
        return;
    }
    
    try {
        // ★★★ [수정 핵심] 파일 경로를 이용해 실제 파일 내용을 읽어옵니다. ★★★
        Path path = Paths.get(filePath);
        String jsonContent = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

        // 2. JSON을 텍스트로 변환 (이제 jsonContent는 진짜 JSON입니다)
        int maxChars = KakaoTalkParseService.getDefaultMaxChars();
        
        // 수정: filePath가 아니라 읽어온 jsonContent를 넘겨줍니다.
        String dialogueText = kakaoTalkParseService.convertJsonToText(jsonContent, maxChars);
        
        // 3. Python 서버로 부정적 반응 트리거 추출 요청
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("text_content", dialogueText);
        requestBody.put("user_name", kakaoName);
        requestBody.put("target_name", targetName);
        requestBody.put("max_chars", maxChars);
        
        Map<String, Object> response = webClient.post()
                .uri("/extract-negative-triggers")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        
        if (response == null || !response.containsKey("negative_triggers")) {
            System.out.println("[ReportCharacter] 부정적 반응 트리거를 찾을 수 없습니다.");
            return;
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> negativeTriggersList = 
            (List<Map<String, Object>>) response.get("negative_triggers");
        
        if (negativeTriggersList == null || negativeTriggersList.isEmpty()) {
            System.out.println("[ReportCharacter] 부정적 반응 트리거가 없습니다.");
            return;
        }
        
        // 4. DTO로 변환
        List<ReactionTriggerDto> negativeTriggers = negativeTriggersList.stream()
            .map(triggerMap -> ReactionTriggerDto.builder()
                .trigger((String) triggerMap.get("trigger"))
                .reaction((String) triggerMap.get("reaction"))
                .example((String) triggerMap.get("example"))
                .build())
            .collect(Collectors.toList());
        
        // 5. ReportCharacter 및 ReportCharacterDetailLog 저장
        saveReportCharacterFromNegativeTriggers(
            character,
            negativeTriggers,
            kakaoName,
            targetName
        );
        
    } catch (Exception e) {
        System.err.println("[ReportCharacter] 리포트 생성 실패: " + e.getMessage());
        e.printStackTrace();
        // 리포트 생성 실패해도 UserCharacter 저장은 성공으로 처리
    }
}
}
