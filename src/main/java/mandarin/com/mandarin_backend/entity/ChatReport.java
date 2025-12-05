package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_report_id")
    private Integer chatReportId;

    @Column(name = "simulation_id")
    private Integer simulationId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "character_id")
    private Integer characterId;

    @Column(name = "chat_report_name", length = 255, nullable = false)
    private String chatReportName;

    @Column(name = "avg_score", nullable = false)
    private Integer avgScore;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;
}

