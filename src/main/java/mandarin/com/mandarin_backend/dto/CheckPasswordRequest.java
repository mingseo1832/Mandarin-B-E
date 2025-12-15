package mandarin.com.mandarin_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckPasswordRequest {
    private Long userId;
    private String password;
}

