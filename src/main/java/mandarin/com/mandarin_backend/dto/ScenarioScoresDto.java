package mandarin.com.mandarin_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 시나리오별 평가 점수
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioScoresDto {
    
    @JsonProperty("metric_1")
    private MetricScoreDto metric1;  // 첫 번째 지표 (ECI 또는 RRI)
    
    @JsonProperty("metric_2")
    private MetricScoreDto metric2;  // 두 번째 지표 (EVR 또는 EEQI)
    
    @JsonProperty("metric_3")
    private MetricScoreDto metric3;  // 세 번째 지표 (CCS 또는 RPS)
}

