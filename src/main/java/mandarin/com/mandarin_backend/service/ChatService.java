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
import java.util.ArrayList;

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
        
        // [수정됨] 방금 만든 안전한 변환 함수 사용! ⭐
        requestBody.put("persona", convertPersonaForPython(persona)); 
        
        requestBody.put("user_message", userMessage);
        
        // history 변환 함수(convertHistoryToList)는 기존에 잘 동작한다면 그대로 두셔도 됩니다.
        // 만약 history 변환 함수가 없다면 이 부분도 확인이 필요합니다.
        requestBody.put("history", convertHistoryToList(history)); 
        
        // 6. 시뮬레이션 컨텍스트 추가
        Map<String, Object> simulationContext = new HashMap<>();
        simulationContext.put("character_age", character.getCharacterAge());
        
        // [수정됨] relation_type: Python이 int를 기대함
        simulationContext.put("relation_type", character.getRelationType());
            
        // [수정됨] meet_date: Python이 Optional[str]을 기대함 (null 허용)
        simulationContext.put("meet_date", 
            character.getMeetDate() != null ? character.getMeetDate().toString() : null);
            
        // [수정됨] love_type: Python이 int를 기대함 (기본값 16)
        simulationContext.put("love_type", 
            character.getLoveType() != null ? character.getLoveType() : 16);
            
        // [수정됨] history_sum: Python이 Optional[str]을 기대함 (null 허용)
        simulationContext.put("history_sum", character.getHistorySum());
            
        // [수정됨] purpose: Python이 "FUTURE" 또는 "PAST"를 기대함
        simulationContext.put("purpose", 
            simulation.getPurpose() != null ? simulation.getPurpose().name() : "FUTURE");
            
        // [수정됨] category: Python이 str을 기대함
        simulationContext.put("category", 
            simulation.getCategory() != null ? simulation.getCategory().name() : "RELATION_TENSION");
        
        requestBody.put("simulation_context", simulationContext);
        
        System.out.println("[Chat] 시뮬레이션 컨텍스트 - 나이: " + character.getCharacterAge()
            + ", 관계: " + character.getRelationType()
            + ", 만난 날짜: " + character.getMeetDate()
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
                            triggerMap.put("keyword", trigger.getKeyword());
                            triggerMap.put("trigger", trigger.getTrigger());
                            triggerMap.put("reaction", trigger.getReaction());
                            triggerMap.put("cause", trigger.getCause());
                            triggerMap.put("solution", trigger.getSolution());
                            triggerMap.put("example", trigger.getExample());
                            return triggerMap;
                        })
                        .collect(Collectors.toList()));
            }
            
            if (reactions.getNegativeTriggers() != null) {
                reactionMap.put("negative_triggers", reactions.getNegativeTriggers().stream()
                        .map(trigger -> {
                            Map<String, String> triggerMap = new HashMap<>();
                            triggerMap.put("keyword", trigger.getKeyword());
                            triggerMap.put("trigger", trigger.getTrigger());
                            triggerMap.put("reaction", trigger.getReaction());
                            triggerMap.put("cause", trigger.getCause());
                            triggerMap.put("solution", trigger.getSolution());
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

    /**
     * Python 서버 규격에 맞춰 페르소나 데이터를 변환 (이름, 말투 + Null 안전 리액션)
     */
    private Map<String, Object> convertPersonaForPython(UserPersonaDto personaDto) {
        Map<String, Object> map = new HashMap<>();
        
        // 1. 기본 정보 (이름) 추가 [기존 코드에서 복구]
        map.put("name", personaDto.getName());

        // 2. 말투 (Speech Style) 추가 [기존 코드에서 복구]
        if (personaDto.getSpeechStyle() != null) {
            Map<String, Object> speechStyleMap = new HashMap<>();
            SpeechStyleDto style = personaDto.getSpeechStyle();
            
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
            map.put("speech_style", speechStyleMap);
        }
        
        // 3. 반응 패턴 (Reaction Patterns) - Python ReactionTrigger 모델에 맞춰 6개 필드 모두 전송
        if (personaDto.getReactionPatterns() != null) {
            Map<String, Object> reactionPatterns = new HashMap<>();
            
            // 3-1. 긍정 패턴 변환 (Python ReactionTrigger: keyword, trigger, reaction, cause, solution, example)
            List<Map<String, String>> positiveList = new ArrayList<>();
            if (personaDto.getReactionPatterns().getPositiveTriggers() != null) {
                for (var item : personaDto.getReactionPatterns().getPositiveTriggers()) {
                    Map<String, String> pyItem = new HashMap<>();
                    
                    // [수정됨] 6개 필드 모두 전송, null이면 빈 문자열로 대치
                    pyItem.put("keyword", item.getKeyword() != null ? item.getKeyword() : "");
                    pyItem.put("trigger", item.getTrigger() != null ? item.getTrigger() : "");
                    pyItem.put("reaction", item.getReaction() != null ? item.getReaction() : "");
                    pyItem.put("cause", item.getCause() != null ? item.getCause() : "");
                    pyItem.put("solution", item.getSolution() != null ? item.getSolution() : "");
                    pyItem.put("example", item.getExample() != null ? item.getExample() : "");
                    
                    positiveList.add(pyItem);
                }
            }
            reactionPatterns.put("positive_triggers", positiveList);

            // 3-2. 부정 패턴 변환 (동일하게 6개 필드 모두 전송)
            List<Map<String, String>> negativeList = new ArrayList<>();
            if (personaDto.getReactionPatterns().getNegativeTriggers() != null) {
                for (var item : personaDto.getReactionPatterns().getNegativeTriggers()) {
                    Map<String, String> pyItem = new HashMap<>();
                    
                    // [수정됨] 6개 필드 모두 전송, null이면 빈 문자열로 대치
                    pyItem.put("keyword", item.getKeyword() != null ? item.getKeyword() : "");
                    pyItem.put("trigger", item.getTrigger() != null ? item.getTrigger() : "");
                    pyItem.put("reaction", item.getReaction() != null ? item.getReaction() : "");
                    pyItem.put("cause", item.getCause() != null ? item.getCause() : "");
                    pyItem.put("solution", item.getSolution() != null ? item.getSolution() : "");
                    pyItem.put("example", item.getExample() != null ? item.getExample() : "");
                    
                    negativeList.add(pyItem);
                }
            }
            reactionPatterns.put("negative_triggers", negativeList);
            
            map.put("reaction_patterns", reactionPatterns);
        }
        
        return map;
    }
}


