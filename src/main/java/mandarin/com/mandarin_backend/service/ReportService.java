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
     * 대화 로그를 시나리오 유형에 따라 분석하여 보고서 생성
     * 
     * @param chatLogs 대화 로그 리스트
     * @param userName 사용자(본인) 이름
     * @param targetName 상대방(페르소나) 이름
     * @param scenarioType 시나리오 유형 ("FUTURE": 미래 시뮬레이션, "PAST": 과거 후회 시뮬레이션)
     * @return 시뮬레이션 분석 보고서
     */
    public ReportResponseDto createReport(List<ChatLogDto> chatLogs, String userName, String targetName, String scenarioType) {
        // Python 서버로 보낼 데이터 준비
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chat_logs", convertChatLogsToList(chatLogs));
        requestBody.put("user_name", userName);
        requestBody.put("target_name", targetName);
        requestBody.put("scenario_type", scenarioType);

        // Python 서버 호출 (POST /report)
        ReportResponseDto response = webClient.post()
                .uri("/report")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(ReportResponseDto.class)
                .block();

        return response;
    }

    /**
     * 대화 로그를 Python 서버 형식의 List로 변환
     */
    private List<Map<String, String>> convertChatLogsToList(List<ChatLogDto> chatLogs) {
        if (chatLogs == null) {
            return List.of();
        }
        
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
     * characterId로 해당 캐릭터의 최신 리포트 조회
     * 
     * @param characterId 캐릭터 ID
     * @return 리포트 응답 DTO (ApiResponse 래핑)
     */
    public ApiResponse<ChatReportResponseDto> getChatReport(Integer characterId) {
        // 해당 캐릭터의 가장 최근 리포트 조회
        ChatReport report = reportRepository.findTopByCharacterIdOrderByCreatedTimeDesc(characterId);

        if (report == null) {
            return ApiResponse.fail("해당 캐릭터의 리포트가 존재하지 않습니다.");
        }

        // Entity -> DTO 변환
        ChatReportResponseDto responseDto = ChatReportResponseDto.builder()
                .chatReportId(report.getChatReportId())
                .simulationId(report.getSimulationId())
                .userId(report.getUserId())
                .characterId(report.getCharacterId())
                .chatReportName(report.getChatReportName())
                .avgScore(report.getAvgScore())
                .createdTime(report.getCreatedTime())
                .build();

        return ApiResponse.success("리포트 조회 성공", responseDto);
    }
}
