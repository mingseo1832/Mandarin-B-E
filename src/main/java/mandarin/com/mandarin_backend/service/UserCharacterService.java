package mandarin.com.mandarin_backend.service;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.UserCharacterResponseDto;
import mandarin.com.mandarin_backend.dto.HistorySumResponseDto;
import mandarin.com.mandarin_backend.entity.UserCharacter;
import mandarin.com.mandarin_backend.exception.UserNotFoundException;
import mandarin.com.mandarin_backend.repository.UserCharacterRepository;
import mandarin.com.mandarin_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UserCharacter 관련 서비스
 */
@Service
@RequiredArgsConstructor
public class UserCharacterService {

    private final UserRepository userRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final WebClient webClient;

    @Transactional(readOnly = true)
    public List<UserCharacterResponseDto> getCharactersByUserId(Long userId) {

        // 1) 유저 존재 여부 체크
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("회원 정보가 없습니다."));

        // 2) 유저에 속한 캐릭터 조회
        List<UserCharacter> characters = userCharacterRepository.findByUserId(userId);

        // 3) 엔티티 → DTO 변환
        return characters.stream()
                .map(UserCharacterResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 히스토리를 요약하여 저장
     */
    @Transactional
    public UserCharacter summarizeAndSaveHistory(Long characterId, String history) {
        UserCharacter character = userCharacterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("캐릭터를 찾을 수 없습니다: " + characterId));

        // Python 서버 호출하여 요약 요청
        Map<String, Object> body = Map.of(
                "history", history,
                "character_name", character.getCharacterName()
        );

        HistorySumResponseDto response = webClient.post()
                .uri("/summarize-history")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(HistorySumResponseDto.class)
                .block();

        if (response == null || response.getSummary() == null) {
            throw new RuntimeException("히스토리 요약에 실패했습니다.");
        }

        character.setHistorySum(response.getSummary());
        return userCharacterRepository.save(character);
    }
}
