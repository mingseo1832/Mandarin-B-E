package mandarin.com.mandarin_backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReportResponseDto {

    private Integer chatReportId;       // 리포트 ID
    private Long simulationId;          // 시뮬레이션 ID
    private Long characterId;           // 캐릭터 ID
    private Integer scoreAvg;           // 평균 점수
    private Integer labelKey;           // 라벨 키
    private Integer labelScore;         // 라벨 점수
    private String reportContent;       // 레포트 내용 (JSON)
    private LocalDateTime createdAt;    // 생성 시간
}
