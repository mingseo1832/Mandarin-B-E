package mandarin.com.mandarin_backend.service;

import mandarin.com.mandarin_backend.dto.UserPersonaDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final WebClient webClient;

    public UserPersonaDto analyzePersona(String textContent, String targetName) {
        // 1. Python 서버로 보낼 데이터 준비
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("text_content", textContent);
        requestBody.put("target_name", targetName);
        requestBody.put("period_days", 14); // 기본값 설정

        // 2. Python 서버 호출 (POST /analyze)
        UserPersonaDto response = webClient.post()
                .uri("/analyze")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(UserPersonaDto.class) // 응답을 DTO로 변환
                .block(); // 결과가 올 때까지 기다림 (동기 처리)

        // 3. (나중에 여기에 DB 저장 코드 추가 예정)
        
        return response;
    }
}