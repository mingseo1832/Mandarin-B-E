package mandarin.com.mandarin_backend.service;

import mandarin.com.mandarin_backend.dto.*;
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

    /**
     * 대화 로그를 분석하여 연애 관점 보고서 생성
     * 
     * @param chatLogs 대화 로그 리스트
     * @param userName 사용자(본인) 이름
     * @param targetName 상대방(페르소나) 이름
     * @return 연애 분석 보고서
     */
    public ReportResponseDto createReport(List<ChatLogDto> chatLogs, String userName, String targetName) {
        // Python 서버로 보낼 데이터 준비
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chat_logs", convertChatLogsToList(chatLogs));
        requestBody.put("user_name", userName);
        requestBody.put("target_name", targetName);

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
}

