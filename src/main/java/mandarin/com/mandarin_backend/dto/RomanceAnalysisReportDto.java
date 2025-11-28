package mandarin.com.mandarin_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RomanceAnalysisReportDto {
    
    // 종합 점수
    @JsonProperty("total_score")
    private int totalScore;
    
    @JsonProperty("score_grade")
    private String scoreGrade;  // "S", "A", "B", "C", "D", "F"
    
    // 상세 점수
    private ConversationScoresDto scores;
    
    // 대화 흐름 분석
    @JsonProperty("flow_direction")
    private String flowDirection;  // "매우 긍정적", "긍정적", "중립", "부정적", "매우 부정적"
    
    @JsonProperty("flow_summary")
    private String flowSummary;
    
    // 주요 순간들
    @JsonProperty("highlight_moments")
    private List<HighlightMomentDto> highlightMoments;
    
    // 호감도 분석
    @JsonProperty("attraction_signals")
    private List<String> attractionSignals;
    
    @JsonProperty("red_flags")
    private List<String> redFlags;
    
    // 종합 피드백
    private List<String> strengths;
    
    private List<String> improvements;
    
    // 다음 대화 조언
    @JsonProperty("next_conversation_tips")
    private List<String> nextConversationTips;
    
    // 총평
    @JsonProperty("overall_assessment")
    private String overallAssessment;
}

