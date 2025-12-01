package mandarin.com.mandarin_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpRequest {
    private String userId;
    private String username;
    private String password;
    private Integer loveType;
}
