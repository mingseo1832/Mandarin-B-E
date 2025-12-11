package mandarin.com.mandarin_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.*;
import mandarin.com.mandarin_backend.entity.Simulation;
import mandarin.com.mandarin_backend.entity.SimulationMessage;
import mandarin.com.mandarin_backend.entity.UserCharacter;
import mandarin.com.mandarin_backend.repository.SimulationMessageRepository;
import mandarin.com.mandarin_backend.repository.SimulationRepository;
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
public class SimulationService {

    private final SimulationRepository simulationRepository;
    private final SimulationMessageRepository simulationMessageRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // character_id로 시뮬레이션 다건 조회
    public ApiResponse<List<SimulationResponseDto>> getSimulationsByCharacterId(Long characterId) {

        List<Simulation> simulations = simulationRepository.findByCharacterCharacterId(characterId);

        if (simulations.isEmpty()) {
            return ApiResponse.success("해당 캐릭터의 시뮬레이션이 없습니다.", List.of());
        }

        // Simulation 엔티티를 SimulationResponseDto로 변환
        List<SimulationResponseDto> responseDtos = simulations.stream()
                .map(simulation -> SimulationResponseDto.builder()
                        .simulationId(simulation.getSimulationId())
                        .characterId(simulation.getCharacter().getCharacterId())
                        .simulationName(simulation.getSimulationName())
                        .purpose(simulation.getPurpose())
                        .category(simulation.getCategory())
                        .time(simulation.getTime())
                        .lastUpdateTime(simulation.getLastUpdateTime())
                        .isFinished(simulation.getIsFinished())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.success("시뮬레이션 정보 조회 성공", responseDtos);
    }

    /**
     * 시뮬레이션 메시지 저장 및 AI 응답 받기
     * 1. 사용자 메시지를 SimulationMessage 테이블에 저장
     * 2. Simulation 테이블의 lastUpdateTime 업데이트
     * 3. AI 응답을 받아서 DB에 저장
     * 4. AI 응답을 프론트에 전송
     */
    @Transactional
    public ApiResponse<SimulationMessageResponseDto> sendMessage(SimulationMessageRequestDto request) {
        try {
            // 1. 시뮬레이션 조회
            Simulation simulation = simulationRepository.findById(request.getSimulationId())
                    .orElseThrow(() -> new IllegalArgumentException("시뮬레이션을 찾을 수 없습니다: " + request.getSimulationId()));

            LocalDateTime now = LocalDateTime.now();

            // 2. 사용자 메시지 저장 (sender: false = USER)
            SimulationMessage userMessage = SimulationMessage.builder()
                    .simulation(simulation)
                    .sender(request.getSender())  // false = 사용자
                    .content(request.getContent())
                    .timestamp(now)
                    .build();
            simulationMessageRepository.save(userMessage);

            // 3. Simulation 테이블의 lastUpdateTime 업데이트
            simulation.setLastUpdateTime(now);
            simulationRepository.save(simulation);

            // 4. 이전 대화 내역 조회
            List<SimulationMessage> messageHistory = simulationMessageRepository
                    .findBySimulationSimulationIdOrderByTimestampAsc(request.getSimulationId());

            List<ChatLogDto> history = messageHistory.stream()
                    .map(msg -> ChatLogDto.builder()
                            .role(msg.getSender() ? "assistant" : "user")
                            .content(msg.getContent())
                            .build())
                    .collect(Collectors.toList());

            // 5. AI에게 보낼 요청 준비
            UserCharacter character = simulation.getCharacter();
            UserPersonaDto persona = parsePersonaFromJson(simulation.getCharacterPersona());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("persona", convertPersonaToMap(persona));
            requestBody.put("user_message", request.getContent());
            requestBody.put("history", convertHistoryToList(history));

            // 시뮬레이션 컨텍스트 추가
            Map<String, Object> simulationContext = new HashMap<>();
            simulationContext.put("character_age", character.getCharacterAge());
            simulationContext.put("relation_type", character.getRelationType());
            simulationContext.put("love_type", character.getLoveType());
            simulationContext.put("history_sum", character.getHistorySum());
            simulationContext.put("purpose", simulation.getPurpose().name());
            simulationContext.put("category", simulation.getCategory().name());
            requestBody.put("simulation_context", simulationContext);

            // 6. Python AI 서버 호출
            ChatResponseDto aiResponse = webClient.post()
                    .uri("/chat")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(ChatResponseDto.class)
                    .block();

            // 7. AI 응답 저장 (sender: true = AI)
            if (aiResponse != null && aiResponse.getReply() != null) {
                LocalDateTime aiTimestamp = LocalDateTime.now();

                SimulationMessage aiMessage = SimulationMessage.builder()
                        .simulation(simulation)
                        .sender(true)  // true = AI
                        .content(aiResponse.getReply())
                        .timestamp(aiTimestamp)
                        .build();
                simulationMessageRepository.save(aiMessage);

                // Simulation의 lastUpdateTime도 AI 응답 시간으로 업데이트
                simulation.setLastUpdateTime(aiTimestamp);
                simulationRepository.save(simulation);

                // 8. 응답 반환
                SimulationMessageResponseDto responseDto = SimulationMessageResponseDto.builder()
                        .sender(true)
                        .content(aiResponse.getReply())
                        .build();

                return ApiResponse.success("메시지 전송 성공", responseDto);
            }

            return ApiResponse.fail("AI 응답을 받지 못했습니다.");

        } catch (Exception e) {
            return ApiResponse.fail("메시지 전송 실패: " + e.getMessage());
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

