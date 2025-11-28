package mandarin.com.mandarin_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestDto {
    
    @JsonProperty("chat_logs")
    private List<ChatLogDto> chatLogs;
    
    @JsonProperty("user_name")
    private String userName;
    
    @JsonProperty("target_name")
    private String targetName;
}

