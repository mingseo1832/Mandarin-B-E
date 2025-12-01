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
public class ChatService {

    private final WebClient webClient;

    /**
     * 페르소나를 적용한 AI와 대화
     * 
     * @param persona 적용할 페르소나 정보
     * @param userMessage 사용자 메시지
     * @param history 이전 대화 내역
     * @return AI 응답
     */
    public ChatResponseDto chat(UserPersonaDto persona, String userMessage, List<ChatLogDto> history) {
        // Python 서버로 보낼 데이터 준비
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("persona", convertPersonaToMap(persona));
        requestBody.put("user_message", userMessage);
        requestBody.put("history", convertHistoryToList(history));

        // Python 서버 호출 (POST /chat)
        ChatResponseDto response = webClient.post()
                .uri("/chat")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(ChatResponseDto.class)
                .block();

        return response;
    }

    /**
     * UserPersonaDto를 Python 서버 형식의 Map으로 변환
     */
    private Map<String, Object> convertPersonaToMap(UserPersonaDto persona) {
        Map<String, Object> personaMap = new HashMap<>();
        personaMap.put("name", persona.getName());
        
        if (persona.getSpeechStyle() != null) {
            Map<String, Object> speechStyleMap = new HashMap<>();
            SpeechStyleDto style = persona.getSpeechStyle();
            
            speechStyleMap.put("politeness_level", style.getPolitenessLevel());
            speechStyleMap.put("tone", style.getTone());
            speechStyleMap.put("common_endings", style.getCommonEndings());
            speechStyleMap.put("frequent_interjections", style.getFrequentInterjections());
            speechStyleMap.put("distinctive_habits", style.getDistinctiveHabits());
            speechStyleMap.put("sample_sentences", style.getSampleSentences());
            
            if (style.getEmojiUsage() != null) {
                Map<String, Object> emojiMap = new HashMap<>();
                emojiMap.put("frequency", style.getEmojiUsage().getFrequency());
                emojiMap.put("preferred_type", style.getEmojiUsage().getPreferredType());
                emojiMap.put("laugh_sound", style.getEmojiUsage().getLaughSound());
                speechStyleMap.put("emoji_usage", emojiMap);
            }
            
            personaMap.put("speech_style", speechStyleMap);
        }
        
        return personaMap;
    }

    /**
     * 대화 내역을 Python 서버 형식의 List로 변환
     */
    private List<Map<String, String>> convertHistoryToList(List<ChatLogDto> history) {
        if (history == null) {
            return List.of();
        }
        
        return history.stream()
                .map(log -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("role", log.getRole());
                    map.put("content", log.getContent());
                    return map;
                })
                .collect(Collectors.toList());
    }
}


