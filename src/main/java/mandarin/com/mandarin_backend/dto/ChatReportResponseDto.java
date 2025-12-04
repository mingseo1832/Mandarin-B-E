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

    private Long reportId;              // 리포트 ID
    private Long characterId;           // 캐릭터 ID
    private String characterName;       // 캐릭터 이름
    private String analysisSummary;     // 분석 요약
    private String suggestedActions;    // JSON 형태의 조언 리스트
    private String visualDataJson;      // 그래프/시각화 데이터
    private LocalDateTime createdAt;    // 생성 시간
}

