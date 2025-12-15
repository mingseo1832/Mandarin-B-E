package mandarin.com.mandarin_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private boolean success;
    private String message;
    private Long id;           // 유저 기본키 (PK)
    private int loveType;
}
