package mandarin.com.mandarin_backend.service;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.HistorySumResponseDto;
import mandarin.com.mandarin_backend.entity.UserCharacter;
import mandarin.com.mandarin_backend.repository.UserCharacterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

/**
 * UserCharacter 관련 서비스
 */
@Service
@RequiredArgsConstructor
public class UserCharacterService {

    private final WebClient webClient;
    private final UserCharacterRepository userCharacterRepository;

    /**
     * 히스토리를 AI로 요약하여 UserCharacter에 저장
     *
     * @param characterId 캐릭터 ID
     * @param history 요약할 히스토리 내용
     * @return 저장된 캐릭터 정보
     */
    @Transactional
    public UserCharacter summarizeAndSaveHistory(Long characterId, String history) {
        // 1. 캐릭터 조회
        UserCharacter character = userCharacterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("캐릭터를 찾을 수 없습니다: " + characterId));

        // 2. Python AI 서버로 히스토리 요약 요청
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("history", history);
        requestBody.put("character_name", character.getCharacterName());

        HistorySumResponseDto response = webClient.post()
                .uri("/summarize-history")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(HistorySumResponseDto.class)
                .block();

        if (response == null || response.getSummary() == null) {
            throw new RuntimeException("히스토리 요약에 실패했습니다.");
        }

        // 3. 요약된 히스토리 저장
        character.setHistorySum(response.getSummary());
        
        return userCharacterRepository.save(character);
    }

    /**
     * 캐릭터 ID로 캐릭터 조회
     *
     * @param characterId 캐릭터 ID
     * @return 캐릭터 정보
     */
    public UserCharacter getCharacterById(Long characterId) {
        return userCharacterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("캐릭터를 찾을 수 없습니다: " + characterId));
    }
}
