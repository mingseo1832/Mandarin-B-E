package mandarin.com.mandarin_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoveTypeRequestDto {
    private Long id;           // 유저 PK (기본키)
    private Integer loveType;  // 0~15: 실제 타입
}

