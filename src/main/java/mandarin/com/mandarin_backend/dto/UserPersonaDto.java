package mandarin.com.mandarin_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserPersonaDto {
    private String name;

    @JsonProperty("speech_style")
    private SpeechStyleDto speechStyle;
    
    /**
     * 상대방 말에 대한 긍정/부정 반응 패턴 분석
     */
    @JsonProperty("reaction_patterns")
    private ReactionAnalysisDto reactionPatterns;
}
