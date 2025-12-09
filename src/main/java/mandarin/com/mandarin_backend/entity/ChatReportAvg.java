package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_report_avg")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReportAvg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_report_avg_id")
    private Integer chatReportAvgId;    // PK

    /**
     * FK → ChatReport
     * 어떤 리포트의 평균 점수인지 연결됨
     */
    @ManyToOne
    @JoinColumn(name = "chat_report_id", nullable = false)
    private ChatReport chatReport;

    /**
     * FK → User
     * 어떤 사용자가 생성한 분석인지 기록
     */
    @ManyToOne
    @JoinColumn(name = "id", nullable = false)
    private User user;

    @Column(name = "avg_mandarin_score", nullable = false)
    private Integer avgMandarinScore;   // 만다린 종합 점수

    /**
     * total_label_key: F1~F6 라벨 키 ENUM
     */
    @Column(name = "total_label_key", nullable = false)
    @Enumerated(EnumType.STRING)
    private TotalLabelKey totalLabelKey;

    // ENUM 선언
    public enum TotalLabelKey {
        F1, F2, F3, F4, F5, F6
    }

    @Column(name = "total_label_score", nullable = false)
    private Integer totalLabelScore;    // 해당 라벨의 점수

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;    // 생성 시간

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
