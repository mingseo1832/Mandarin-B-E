package mandarin.com.mandarin_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mandarin.com.mandarin_backend.entity.User;
import mandarin.com.mandarin_backend.entity.UserCharacter;
import mandarin.com.mandarin_backend.exception.UserNotFoundException;
import mandarin.com.mandarin_backend.repository.UserCharacterRepository;
import mandarin.com.mandarin_backend.repository.UserRepository;
import mandarin.com.mandarin_backend.dto.UserCharacterResponseDto;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCharacterService {

    private final UserRepository userRepository;
    private final UserCharacterRepository userCharacterRepository;

    @Transactional(readOnly = true)
    public List<UserCharacterResponseDto> getCharactersByUserId(Long userId) {

        // 1) 유저 존재 여부 체크
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("회원 정보가 없습니다."));

        // 2) 유저에 속한 캐릭터 조회
        List<UserCharacter> characters = userCharacterRepository.findByUser_Id(userId);

        // 3) 엔티티 → DTO 변환
        return characters.stream()
                .map(c -> UserCharacterResponseDto.builder()
                        .characterId(c.getCharacterId())
                        .characterName(c.getCharacterName())
                        .kakaoName(c.getKakaoName())
                        .build())
                .collect(Collectors.toList());
    }
}
