package mandarin.com.mandarin_backend.service;

import mandarin.com.mandarin_backend.dto.ParseInfoResponseDto;
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

    /**
     * 카카오톡 대화 내용에서 페르소나 추출
     * 
     * @param textContent 카카오톡 대화 텍스트
     * @param targetName 분석 대상 인물 이름
     * @return 추출된 페르소나 정보
     */
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
                .bodyToMono(UserPersonaDto.class)
                .block();

        // 3. (나중에 여기에 DB 저장 코드 추가 예정)
        
        return response;
    }

    /**
     * 카카오톡 대화 파일의 파싱 정보 조회
     * (참여자 목록, 대화 기간, 메시지 수 등)
     * 
     * @param textContent 카카오톡 대화 텍스트
     * @return 파싱 정보
     */
    public ParseInfoResponseDto parseInfo(String textContent) {
        // Python 서버로 보낼 데이터 준비
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("text_content", textContent);

        // Python 서버 호출 (POST /parse-info)
        ParseInfoResponseDto response = webClient.post()
                .uri("/parse-info")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(ParseInfoResponseDto.class)
                .block();

        return response;
    }
}