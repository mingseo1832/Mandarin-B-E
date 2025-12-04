package mandarin.com.mandarin_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 개별 평가 지표 점수
 * 
 * 미래 시뮬레이션: ECI(관계 유지력), EVR(감정 안정성), CCS(선택 일관성)
 * 과거 후회 시뮬레이션: RRI(후회 해소도), EEQI(감정 표현 성숙도), RPS(관계 회복력)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricScoreDto {
    
    private String code;    // 지표 코드 (ECI, EVR, CCS 또는 RRI, EEQI, RPS)
    private String name;    // 지표 이름
    private int score;      // 점수 (0-100)
    private String reason;  // 점수 산정 이유
    
    @JsonProperty("key_conversations")
    private List<KeyConversationDto> keyConversations;  // 점수 산정에 영향을 준 주요 대화
}
