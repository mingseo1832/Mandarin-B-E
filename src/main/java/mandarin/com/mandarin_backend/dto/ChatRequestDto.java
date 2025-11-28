package mandarin.com.mandarin_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDto {
    
    private UserPersonaDto persona;
    
    @JsonProperty("user_message")
    private String userMessage;
    
    @Builder.Default
    private List<ChatLogDto> history = new ArrayList<>();
}

