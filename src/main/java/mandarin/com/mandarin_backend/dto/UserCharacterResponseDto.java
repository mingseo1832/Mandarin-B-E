package mandarin.com.mandarin_backend.dto;

import lombok.*;
import mandarin.com.mandarin_backend.entity.UserCharacter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCharacterResponseDto {

    private Long characterId;
    private Long userId;
    private String characterName;
    private int characterAge;
    private int relationType;
    private String characterImg;
    private LocalDateTime meetDate;
    private Integer loveType;
    private String kakaoName;
    private String fullDialogue;
    private LocalDateTime createdAt;
    private String historySum;

    public static UserCharacterResponseDto fromEntity(UserCharacter c) {
        return UserCharacterResponseDto.builder()
                .characterId(c.getCharacterId())
                .userId(c.getUser().getId())
                .characterName(c.getCharacterName())
                .characterAge(c.getCharacterAge())
                .relationType(c.getRelationType())
                .characterImg(c.getCharacterImg())
                .meetDate(c.getMeetDate())
                .loveType(c.getLoveType())
                .kakaoName(c.getKakaoName())
                .fullDialogue(c.getFullDialogue())
                .createdAt(c.getCreatedAt())
                .historySum(c.getHistorySum())
                .build();
    }
}
