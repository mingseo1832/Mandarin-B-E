package mandarin.com.mandarin_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private Long id;
    private boolean success;
    private String message;
    private int loveType;
}
