package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Chat_Report_Score")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReportScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_report_score_id")
    private Integer chatReportScoreId;  // PK

    /**
     * FK → ChatReport
     */
    @ManyToOne
    @JoinColumn(name = "chat_report_id", nullable = false)
    private ChatReport chatReport;

    /**
     * 라벨 (F1~F6, P1~P3 등)
     */
    @Column(name = "label", nullable = false, length = 10)
    private String label;

    @Column(name = "label_score", nullable = false)
    private Integer labelScore;
}
