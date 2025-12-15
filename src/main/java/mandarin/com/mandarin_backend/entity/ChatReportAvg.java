package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Chat_Report_Avg")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReportAvg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_report_avg_id")
    private Long chatReportAvgId;  // PK

    /**
     * FK → ChatReport
     */
    @ManyToOne
    @JoinColumn(name = "chat_report_id", nullable = false)
    private ChatReport chatReport;

    /**
     * FK → User
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 만다린 전체 평균 점수
     */
    @Column(name = "avg_mandarin_score", nullable = false)
    private Integer avgMandarinScore;

    /**
     * 6개 라벨(F1~F6)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "total_label_key", nullable = false, length = 3)
    private TotalLabelKey totalLabelKey;

    /**
     * 해당 라벨 점수
     */
    @Column(name = "total_label_score", nullable = false)
    private Integer totalLabelScore;

    /**
     * 생성 시간
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    /**
     * ENUM 정의 — F1~F6
     */
    public enum TotalLabelKey {
        F1, F2, F3, P1, P2, P3
    }
}
