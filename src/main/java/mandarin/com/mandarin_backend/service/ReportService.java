package mandarin.com.mandarin_backend.service;

import mandarin.com.mandarin_backend.dto.*;
import mandarin.com.mandarin_backend.entity.ChatReport;
import mandarin.com.mandarin_backend.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final WebClient webClient;
    private final ReportRepository reportRepository;

    /**
     * 대화 로그를 시나리오 유형(FUTURE/PAST)에 따라 분석하여 보고서 생성
     * Python 서버로 대화 내용을 보내고 분석 결과 DTO를 그대로 반환
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
        ChatReportResponseDto responseDto = ChatReportResponseDto.fromEntity(report);

        return ApiResponse.success("리포트 조회 성공", responseDto);
    }

    /**
     * 시뮬레이션 ID 기준 리포트 조회
     */
    public ApiResponse<ChatReportResponseDto> getChatReportBySimulationId(Long simulationId) {

        return reportRepository.findBySimulationId(simulationId)
                .map(report -> {
                    ChatReportResponseDto dto = ChatReportResponseDto.fromEntity(report);
                    return ApiResponse.success("리포트 조회 성공", dto);
                })
                .orElse(ApiResponse.fail("해당 시뮬레이션의 리포트가 존재하지 않습니다."));
    }
}
