package mandarin.com.mandarin_backend.service;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.HistorySumResponseDto;
import mandarin.com.mandarin_backend.dto.UserCharacterRequestDto;
import mandarin.com.mandarin_backend.dto.UserCharacterResponseDto;
import mandarin.com.mandarin_backend.entity.User;
import mandarin.com.mandarin_backend.entity.UserCharacter;
import mandarin.com.mandarin_backend.exception.CharacterNotFoundException;
import mandarin.com.mandarin_backend.exception.UserNotFoundException;
import mandarin.com.mandarin_backend.repository.UserCharacterRepository;
import mandarin.com.mandarin_backend.repository.UserRepository;
import mandarin.com.mandarin_backend.util.FileUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCharacterService {

    private final WebClient webClient;
    private final UserRepository userRepository;
    private final UserCharacterRepository characterRepository;
    private final FileUtil fileUtil;

    // ================================
    // 1. 캐릭터 다건 조회 (GET /character/user/{id})
    // ================================
    public List<UserCharacterResponseDto> getCharactersByUserId(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("회원 정보가 없습니다."));

        List<UserCharacter> characters = characterRepository.findByUser(user);

        return characters.stream()
                .map(UserCharacterResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    // ================================
    // 2. 캐릭터 단건 조회 (GET /character/{characterId})
    // ================================
    public UserCharacterResponseDto getCharacter(Long characterId) {

        UserCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new CharacterNotFoundException("캐릭터 정보가 없습니다."));

        return UserCharacterResponseDto.fromEntity(character);
    }

    // ================================
    // 3. 캐릭터 생성 (POST /character/create)
    // ================================
    @Transactional
    public void createCharacter(UserCharacterRequestDto dto,
                                MultipartFile characterImg,
                                MultipartFile fullDialogueFile) throws IOException {

        User user = userRepository.findById(dto.getId())
                .orElseThrow(() -> new UserNotFoundException("회원 정보가 없습니다."));

        String characterImgPath = fileUtil.saveFile(characterImg);
        String dialoguePath = fileUtil.saveFile(fullDialogueFile);

        UserCharacter newChar = UserCharacter.builder()
                .user(user)
                .characterName(dto.getCharacterName())
                .characterAge(dto.getCharacterAge())
                .relationType(dto.getRelationType())
                .meetDate(dto.getMeetDate() != null ? LocalDateTime.parse(dto.getMeetDate()) : null)
                .loveType(dto.getLoveType())
                .historySum(dto.getHistorySum())
                .kakaoName(dto.getKakaoName())
                .characterImg(characterImgPath)
                .fullDialogue(dialoguePath)
                .createdAt(LocalDateTime.now())
                .build();

        characterRepository.save(newChar);

        // TODO: Report_Character 테이블 생성 (AI 사용)
    }

    // ================================
    // 4. 캐릭터 수정 (POST /character/update/{characterId})
    // ================================
    @Transactional
    public void updateCharacter(Long characterId,
                                UserCharacterRequestDto dto,
                                MultipartFile characterImg,
                                MultipartFile fullDialogueFile) throws IOException {

        UserCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new CharacterNotFoundException("캐릭터 정보가 없습니다."));

        if (dto.getCharacterName() != null) character.setCharacterName(dto.getCharacterName());
        if (dto.getCharacterAge() != 0) character.setCharacterAge(dto.getCharacterAge());
        if (dto.getRelationType() != 0) character.setRelationType(dto.getRelationType());
        if (dto.getMeetDate() != null) character.setMeetDate(LocalDateTime.parse(dto.getMeetDate()));
        if (dto.getLoveType() != null) character.setLoveType(dto.getLoveType());
        if (dto.getHistorySum() != null) character.setHistorySum(dto.getHistorySum());
        if (dto.getKakaoName() != null) character.setKakaoName(dto.getKakaoName());

        // 새로운 파일 들어오면 변경
        if (characterImg != null && !characterImg.isEmpty()) {
            fileUtil.deleteFile(character.getCharacterImg());
            character.setCharacterImg(fileUtil.saveFile(characterImg));
        }

        if (fullDialogueFile != null && !fullDialogueFile.isEmpty()) {
            fileUtil.deleteFile(character.getFullDialogue());
            character.setFullDialogue(fileUtil.saveFile(fullDialogueFile));
        }

        // TODO: Report_Character 수정(AI)
    }

    // ================================
    // 5. 캐릭터 삭제 (DELETE /character/delete/{characterId})
    // ================================
    @Transactional
    public void deleteCharacter(Long characterId) {

        UserCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new CharacterNotFoundException("캐릭터 정보가 없습니다."));

        fileUtil.deleteFile(character.getCharacterImg());
        fileUtil.deleteFile(character.getFullDialogue());

        characterRepository.delete(character);

        // TODO: Report_Character 삭제 처리
    }

    // ================================
    // 6. 히스토리 요약 (기존 기능 유지)
    // ================================
    @Transactional
    public UserCharacter summarizeAndSaveHistory(Long characterId, String history) {

        UserCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new CharacterNotFoundException("캐릭터를 찾을 수 없습니다: " + characterId));

        Map<String, String> body = new HashMap<>();
        body.put("history", history);
        body.put("character_name", character.getCharacterName());

        HistorySumResponseDto response = webClient.post()
                .uri("/summarize-history")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(HistorySumResponseDto.class)
                .block();

        if (response == null || response.getSummary() == null) {
            throw new RuntimeException("히스토리 요약 실패.");
        }

        character.setHistorySum(response.getSummary());
        return characterRepository.save(character);
    }

    public UserCharacter getCharacterById(Long characterId) {
        return characterRepository.findById(characterId)
                .orElseThrow(() -> new CharacterNotFoundException("캐릭터를 찾을 수 없습니다: " + characterId));
    }
}
