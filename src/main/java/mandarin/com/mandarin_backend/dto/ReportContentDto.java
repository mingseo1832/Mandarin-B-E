package mandarin.com.mandarin_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 리포트 상세 내용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportContentDto {
    
    private String analysis;  // 전반적인 대화 흐름과 사용자 태도에 대한 상세 분석 (200자 내외)
    
    private String feedback;  // 더 나은 결과를 위해 사용자에게 주는 구체적인 조언
    
    @JsonProperty("overall_rating")
    private int overallRating;  // 전체 리포트 점수의 평균값 (0-100)
}

