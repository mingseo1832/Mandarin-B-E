package mandarin.com.mandarin_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCharacterRequestDto {

    private Long id;  // user_id
    private String characterName;
    private int characterAge;
    private int relationType;
    private String meetDate; // 문자열로 받음
    private Integer loveType;
    private String historySum;
    private String kakaoName;
}
