package mandarin.com.mandarin_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationScoresDto {
    
    @JsonProperty("overall_flow")
    private int overallFlow;
    
    @JsonProperty("emotional_connection")
    private int emotionalConnection;
    
    @JsonProperty("interest_signal")
    private int interestSignal;
    
    @JsonProperty("conversation_skill")
    private int conversationSkill;
    
    @JsonProperty("timing_response")
    private int timingResponse;
}


