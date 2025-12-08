package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Chat_Report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_report_id")
    private Integer chatReportId;  // PK

    /**
     * FK → Simulation
     */
    @ManyToOne
    @JoinColumn(name = "simulation_id", nullable = false)
    private Simulation simulation;

    @Column(name = "score_avg", nullable = false)
    private Integer scoreAvg;

    @Column(name = "label_key", nullable = false)
    private Integer labelKey;

    @Column(name = "label_score", nullable = false)
    private Integer labelScore;

    @Column(name = "report_content", nullable = false, columnDefinition = "LONGTEXT")
    private String reportContent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
