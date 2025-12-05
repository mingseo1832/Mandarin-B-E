package mandarin.com.mandarin_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReportResponseDto {

    private Integer chatReportId;       // 리포트 ID
    private Integer simulationId;       // 시뮬레이션 ID
    private Integer userId;             // 사용자 ID
    private Integer characterId;        // 캐릭터 ID
    private String chatReportName;      // 리포트 이름
    private Integer avgScore;           // 평균 점수
    private LocalDateTime createdTime;  // 생성 시간
}
