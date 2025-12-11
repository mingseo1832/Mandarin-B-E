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

    /**
     * FK → User
     */
    @ManyToOne
    @JoinColumn(name = "id")
    private User user;

    /**
     * FK → UserCharacter
     */
    @ManyToOne
    @JoinColumn(name = "character_id")
    private UserCharacter character;

    @Column(name = "score_avg", nullable = false)
    private Integer scoreAvg;

    @Column(name = "label_key", nullable = false) // 평가지표 (관계유지력/감정안정성/선택일관성/후회해소도/감정표현성숙도/관계회복력)
    private Integer labelKey;

    @Column(name = "label_score", nullable = false) // 평가별 점수
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
