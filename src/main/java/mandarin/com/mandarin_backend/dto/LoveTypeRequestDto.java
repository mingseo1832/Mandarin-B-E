package mandarin.com.mandarin_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoveTypeRequestDto {
    private String userId;     // 유저 아이디
    private Integer loveType;  // 0~15: 실제 타입
}

