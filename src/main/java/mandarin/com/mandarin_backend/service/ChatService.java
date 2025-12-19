package mandarin.com.mandarin_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mandarin.com.mandarin_backend.dto.*;
import mandarin.com.mandarin_backend.entity.Simulation;
import mandarin.com.mandarin_backend.entity.SimulationMessage;
import mandarin.com.mandarin_backend.entity.UserCharacter;
import mandarin.com.mandarin_backend.repository.SimulationMessageRepository;
import mandarin.com.mandarin_backend.repository.SimulationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final WebClient webClient;
    private final SimulationRepository simulationRepository;
    private final SimulationMessageRepository simulationMessageRepository;
    private final ObjectMapper objectMapper;

    /**
     * 시뮬레이션 ID를 기반으로 AI와 대화
     * Simulation과 UserCharacter의 정보를 조회하여 AI에게 컨텍스트를 전달합니다.
     * 사용자 메시지와 AI 응답을 SimulationMessage에 저장합니다.
     * 
     * @param simulationId 시뮬레이션 ID
     * @param userMessage 사용자 메시지
     * @param history 이전 대화 내역
     * @return AI 응답
     */
    @Transactional
    public ChatResponseDto chat(Long simulationId, String userMessage, List<ChatLogDto> history) {
        // 1. 시뮬레이션 조회
        Simulation simulation = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new IllegalArgumentException("시뮬레이션을 찾을 수 없습니다: " + simulationId));
        
        // 2. UserCharacter 조회 (Simulation에서 가져옴)
        UserCharacter character = simulation.getCharacter();
        
        // 3. 페르소나 JSON 파싱
        UserPersonaDto persona = parsePersonaFromJson(simulation.getCharacterPersona());
        
        // 4. 사용자 메시지 저장 (sender: "user" = 사용자)
        LocalDateTime userMessageTime = LocalDateTime.now();
        SimulationMessage userMsg = SimulationMessage.builder()
                .simulation(simulation)
                .sender("user")  // "user" = 사용자
                .content(userMessage)
                .timestamp(userMessageTime)
                .build();
        simulationMessageRepository.save(userMsg);
        
        System.out.println("[Chat] 사용자 메시지 저장 - 시뮬레이션ID: " + simulationId + ", 시간: " + userMessageTime);
        
        // 5. Python 서버로 보낼 데이터 준비
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("persona", convertPersonaToMap(persona));
        requestBody.put("user_message", userMessage);
        requestBody.put("history", convertHistoryToList(history));
        
        // 6. 시뮬레이션 컨텍스트 추가
        Map<String, Object> simulationContext = new HashMap<>();
        simulationContext.put("character_age", character.getCharacterAge());
        simulationContext.put("relation_type", character.getRelationType());
        simulationContext.put("meet_date", character.getMeetDate());
        simulationContext.put("love_type", character.getLoveType());
        simulationContext.put("history_sum", character.getHistorySum());
        simulationContext.put("purpose", simulation.getPurpose().name());
        simulationContext.put("category", simulation.getCategory().name());
        
        requestBody.put("simulation_context", simulationContext);

        System.out.println("[Chat] 시뮬레이션 컨텍스트 - 나이: " + character.getCharacterAge()
            + ", 관계: " + character.getRelationType()
            + ", 러브타입: " + character.getLoveType()
            + ", 목적: " + simulation.getPurpose()
            + ", 카테고리: " + simulation.getCategory());

        // 7. Python 서버 호출 (POST /chat)
       try{ ChatResponseDto response = webClient.post()
                .uri("/chat")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(ChatResponseDto.class)
                .block();

        // 8. AI 응답 저장 (sender: "assistant" = AI (캐릭터))
        if (response != null && response.getReply() != null) {
            LocalDateTime aiResponseTime = LocalDateTime.now();
            SimulationMessage aiMsg = SimulationMessage.builder()
                    .simulation(simulation)
                    .sender("assistant")  // "assistant" = AI (캐릭터)
                    .content(response.getReply())
                    .timestamp(aiResponseTime)
                    .build();
            simulationMessageRepository.save(aiMsg);
            
            System.out.println("[Chat] AI 응답 저장 - 시뮬레이션ID: " + simulationId + ", 시간: " + aiResponseTime);
        }

        return response;

} catch (WebClientResponseException.UnprocessableEntity e) {
    // ⭐ 여기가 핵심입니다! Python이 알려주는 에러 원인을 출력합니다.
    String errorBody = e.getResponseBodyAsString();
    System.err.println("==========================================");
    System.err.println("🚨 [Python 422 에러 상세 내용] 🚨");
    System.err.println("내용: " + errorBody);
    System.err.println("==========================================");
    throw e; // 에러를 다시 던져서 상위 처리에 맡김
}
    }

    /**
     * JSON 문자열에서 UserPersonaDto 파싱
     */
    private UserPersonaDto parsePersonaFromJson(String personaJson) {
        try {
            return objectMapper.readValue(personaJson, UserPersonaDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("페르소나 JSON 파싱 실패: " + e.getMessage());
        }
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
        
        // 긍정/부정 반응 패턴 추가
        if (persona.getReactionPatterns() != null) {
            Map<String, Object> reactionMap = new HashMap<>();
            ReactionAnalysisDto reactions = persona.getReactionPatterns();
            
            if (reactions.getPositiveTriggers() != null) {
                reactionMap.put("positive_triggers", reactions.getPositiveTriggers().stream()
                        .map(trigger -> {
                            Map<String, String> triggerMap = new HashMap<>();
                            triggerMap.put("trigger", trigger.getTrigger());
                            triggerMap.put("reaction", trigger.getReaction());
                            triggerMap.put("example", trigger.getExample());
                            return triggerMap;
                        })
                        .collect(Collectors.toList()));
            }
            
            if (reactions.getNegativeTriggers() != null) {
                reactionMap.put("negative_triggers", reactions.getNegativeTriggers().stream()
                        .map(trigger -> {
                            Map<String, String> triggerMap = new HashMap<>();
                            triggerMap.put("trigger", trigger.getTrigger());
                            triggerMap.put("reaction", trigger.getReaction());
                            triggerMap.put("example", trigger.getExample());
                            return triggerMap;
                        })
                        .collect(Collectors.toList()));
            }
            
            personaMap.put("reaction_patterns", reactionMap);
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


