package mandarin.com.mandarin_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HighlightChatMessageDto {
    private String role;    // "user" 또는 "target"
    private String content;
}

