package mandarin.com.mandarin_backend.dto;

import lombok.*;
import mandarin.com.mandarin_backend.entity.UserCharacter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCharacterResponseDto {

    private Long characterId;     // PK
    private String characterName; // 캐릭터 이름
    private String kakaoName;     // 카카오톡 이름

    // 필요하면 나중에 확장 가능
    // private int characterAge;
    // private int relationType;
    // private String characterImg;
    // private Integer loveType;
    // private String historySum;

    public static UserCharacterResponseDto fromEntity(UserCharacter entity) {
        return UserCharacterResponseDto.builder()
                .characterId(entity.getCharacterId())
                .characterName(entity.getCharacterName())
                .kakaoName(entity.getKakaoName())
                .build();
    }
}
