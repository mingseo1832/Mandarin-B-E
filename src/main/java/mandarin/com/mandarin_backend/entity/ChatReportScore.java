package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_report_score")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReportScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_report_score_id")
    private Integer chatReportScoreId;

    @Column(name = "chat_report_id")
    private Integer chatReportId;

    @Column(name = "label", nullable = false)
    private String label; // ENUM(F1~P3)

    @Column(name = "label_score", nullable = false)
    private Integer labelScore;
}

