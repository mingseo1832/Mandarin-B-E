package mandarin.com.mandarin_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 시뮬레이션 대화 분석 보고서
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationReportDto {
    
    private String summary;  // 대화 내용에 대한 3줄 요약
    
    @JsonProperty("scenario_type")
    private String scenarioType;  // 시나리오 유형 ("FUTURE" 또는 "PAST")
    
    private ScenarioScoresDto scores;  // 시나리오별 평가 점수
    
    private ReportContentDto report;  // 리포트 상세 내용
}

