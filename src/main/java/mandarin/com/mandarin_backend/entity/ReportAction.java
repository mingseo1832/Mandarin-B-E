package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_action")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id")
    private Integer actionId;

    @Column(name = "character_id")
    private Integer characterId;

    @Column(name = "action_name", length = 100, nullable = false)
    private String actionName;

    @Column(name = "act_description", columnDefinition = "TEXT", nullable = false)
    private String actDescription;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;
}

