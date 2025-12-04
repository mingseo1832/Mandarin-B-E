package mandarin.com.mandarin_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParseInfoResponseDto {
    
    @JsonProperty("format_type")
    private String formatType;
    
    @JsonProperty("total_days")
    private int totalDays;
    
    @JsonProperty("total_messages")
    private int totalMessages;
    
    private List<String> participants;
    
    @JsonProperty("participant_count")
    private int participantCount;
    
    @JsonProperty("date_range")
    private Map<String, String> dateRange;
}

