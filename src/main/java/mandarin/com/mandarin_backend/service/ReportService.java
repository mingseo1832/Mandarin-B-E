package mandarin.com.mandarin_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mandarin.com.mandarin_backend.dto.*;
import mandarin.com.mandarin_backend.entity.ChatReport;
import mandarin.com.mandarin_backend.entity.Simulation;
import mandarin.com.mandarin_backend.entity.User;
import mandarin.com.mandarin_backend.entity.UserCharacter;
import mandarin.com.mandarin_backend.repository.ReportRepository;
import mandarin.com.mandarin_backend.repository.SimulationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final WebClient webClient;
    private final ReportRepository reportRepository;
    private final SimulationRepository simulationRepository;
    private final ObjectMapper objectMapper;

    /**
     * 대화 로그를 시나리오 유형(FUTURE/PAST)에 따라 분석하여 보고서 생성 및 DB 저장
     * Python 서버로 대화 내용을 보내고 분석 결과를 ChatReport 엔티티에 저장
     * 
     * @param simulationId 시뮬레이션 ID
     * @param chatLogs 대화 로그
     * @param userName 사용자 이름
     * @param targetName 상대방 이름
     * @param scenarioType 시나리오 유형 ("FUTURE" 또는 "PAST")
     * @return 리포트 응답 DTO
     */
    @Transactional
    public ReportResponseDto createReportAndSave(Long simulationId, List<ChatLogDto> chatLogs, 
                                                  String userName, String targetName, String scenarioType) {
        
        // 1. 시뮬레이션 조회
        Simulation simulation = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new IllegalArgumentException("시뮬레이션을 찾을 수 없습니다: " + simulationId));
        
        UserCharacter character = simulation.getCharacter();
        User user = character.getUser();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chat_logs", convertChatLogsToList(chatLogs));
        requestBody.put("user_name", userName);
        requestBody.put("target_name", targetName);
        requestBody.put("scenario_type", scenarioType);

        // 2. Python 서버 호출
        ReportResponseDto response = webClient.post()
                .uri("/report")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(ReportResponseDto.class)
                .block();

        // 3. 리포트 DB 저장
        if (response != null && response.getReport() != null) {
            saveReportToDb(simulation, user, character, response.getReport(), scenarioType);
            System.out.println("[Report] 리포트 저장 완료 - 시뮬레이션ID: " + simulationId);
        }

        return response;
    }

    /**
     * 리포트를 ChatReport 엔티티에 저장
     * 각 평가 지표(metric)별로 별도의 레코드로 저장
     */
    private void saveReportToDb(Simulation simulation, User user, UserCharacter character,
                                SimulationReportDto report, String scenarioType) {
        
        ScenarioScoresDto scores = report.getScores();
        int overallRating = report.getReport().getOverallRating();
        LocalDateTime now = LocalDateTime.now();
        
        // 전체 리포트 내용을 JSON으로 변환
        String reportContentJson = convertReportToJson(report);
        
        // 각 metric별로 저장 (3개의 레코드)
        // metric_1: ECI(관계유지력) 또는 RRI(후회해소도)
        saveMetricReport(simulation, user, character, scores.getMetric1(), 
                        getLabelKey(scores.getMetric1().getCode()), overallRating, reportContentJson, now);
        
        // metric_2: EVR(감정안정성) 또는 EEQI(감정표현성숙도)
        saveMetricReport(simulation, user, character, scores.getMetric2(),
                        getLabelKey(scores.getMetric2().getCode()), overallRating, reportContentJson, now);
        
        // metric_3: CCS(선택일관성) 또는 RPS(관계회복력)
        saveMetricReport(simulation, user, character, scores.getMetric3(),
                        getLabelKey(scores.getMetric3().getCode()), overallRating, reportContentJson, now);
    }

    /**
     * 개별 metric 리포트 저장
     */
    private void saveMetricReport(Simulation simulation, User user, UserCharacter character,
                                  MetricScoreDto metric, int labelKey, int scoreAvg, 
                                  String reportContent, LocalDateTime createdAt) {
        ChatReport chatReport = ChatReport.builder()
                .simulation(simulation)
                .user(user)
                .character(character)
                .scoreAvg(scoreAvg)
                .labelKey(labelKey)
                .labelScore(metric.getScore())
                .reportContent(reportContent)
                .createdAt(createdAt)
                .build();
        
        reportRepository.save(chatReport);
    }

    /**
     * 지표 코드를 labelKey 숫자로 변환
     * FUTURE: ECI(1), EVR(2), CCS(3)
     * PAST: RRI(4), EEQI(5), RPS(6)
     */
    private int getLabelKey(String code) {
        return switch (code) {
            case "ECI" -> 1;  // 관계 유지력
            case "EVR" -> 2;  // 감정 안정성
            case "CCS" -> 3;  // 선택 일관성
            case "RRI" -> 4;  // 후회 해소도
            case "EEQI" -> 5; // 감정 표현 성숙도
            case "RPS" -> 6;  // 관계 회복력
            default -> 0;
        };
    }

    /**
     * 리포트 DTO를 JSON 문자열로 변환
     */
    private String convertReportToJson(SimulationReportDto report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            System.err.println("리포트 JSON 변환 실패: " + e.getMessage());
            return "{}";
        }
    }

    /**
     * 대화 로그를 시나리오 유형(FUTURE/PAST)에 따라 분석하여 보고서 생성
     * Python 서버로 대화 내용을 보내고 분석 결과 DTO를 그대로 반환 (저장 없음)
     * @deprecated createReportAndSave 사용 권장
     */
    public ReportResponseDto createReport(List<ChatLogDto> chatLogs, String userName, String targetName, String scenarioType) {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chat_logs", convertChatLogsToList(chatLogs));
        requestBody.put("user_name", userName);
        requestBody.put("target_name", targetName);
        requestBody.put("scenario_type", scenarioType);

        // Python 서버 호출
        return webClient.post()
                .uri("/report")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(ReportResponseDto.class)
                .block();
    }

    /**
     * ChatLogDto 리스트를 Python 서버 정의 스펙에 맞게 변환
     */
    private List<Map<String, String>> convertChatLogsToList(List<ChatLogDto> chatLogs) {
        if (chatLogs == null) return List.of();

        return chatLogs.stream()
                .map(log -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("role", log.getRole());
                    map.put("content", log.getContent());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * 캐릭터 ID 기준 최신 리포트 조회
     */
    public ApiResponse<ChatReportResponseDto> getChatReport(Long characterId) {

        // Repository에서 단일 최신 리포트 1개 조회
        ChatReport report = reportRepository
                .findFirstBySimulation_Character_CharacterIdOrderByCreatedAtDesc(characterId);

        if (report == null) {
            return ApiResponse.fail("해당 캐릭터의 리포트가 존재하지 않습니다.");
        }

        // Entity → DTO 변환
        ChatReportResponseDto responseDto = ChatReportResponseDto.builder()
                .chatReportId(report.getChatReportId())
                .simulationId(report.getSimulation().getSimulationId())
                .characterId(report.getSimulation().getCharacter().getCharacterId())
                .scoreAvg(report.getScoreAvg())
                .labelKey(report.getLabelKey())
                .labelScore(report.getLabelScore())
                .reportContent(report.getReportContent())
                .createdAt(report.getCreatedAt())
                .build();

        return ApiResponse.success("리포트 조회 성공", responseDto);
    }

    /**
     * 시뮬레이션 ID 기준 리포트 조회
     */
    public ApiResponse<ChatReportResponseDto> getChatReportBySimulationId(Long simulationId) {

        return reportRepository.findBySimulationId(simulationId)
                .map(report -> {

                    ChatReportResponseDto dto = ChatReportResponseDto.builder()
                            .chatReportId(report.getChatReportId())
                            .simulationId(report.getSimulation().getSimulationId())
                            .characterId(report.getSimulation().getCharacter().getCharacterId())
                            .scoreAvg(report.getScoreAvg())
                            .labelKey(report.getLabelKey())
                            .labelScore(report.getLabelScore())
                            .reportContent(report.getReportContent())
                            .createdAt(report.getCreatedAt())
                            .build();

                    return ApiResponse.success("리포트 조회 성공", dto);

                })
                .orElse(ApiResponse.fail("해당 시뮬레이션의 리포트가 존재하지 않습니다."));
    }
}
