package mandarin.com.mandarin_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HighlightMomentDto {
    
    @JsonProperty("moment_type")
    private String momentType;  // "positive", "negative", "neutral"
    
    private List<HighlightChatMessageDto> conversation;
    
    private String analysis;
    
    private String suggestion;  // nullable
}


