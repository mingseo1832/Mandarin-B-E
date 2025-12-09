package mandarin.com.mandarin_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 긍정/부정 반응 분석 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionAnalysisDto {
    
    /**
     * 상대방(사용자)의 어떤 말/행동에 긍정적으로 반응했는지 TOP 3
     */
    @JsonProperty("positive_triggers")
    private List<ReactionTriggerDto> positiveTriggers;
    
    /**
     * 상대방(사용자)의 어떤 말/행동에 부정적으로 반응했는지 TOP 3
     */
    @JsonProperty("negative_triggers")
    private List<ReactionTriggerDto> negativeTriggers;
}
