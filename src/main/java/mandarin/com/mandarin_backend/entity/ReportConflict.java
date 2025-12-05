package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_conflict")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportConflict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conflict_id")
    private Integer conflictId;

    @Column(name = "character_id")
    private Integer characterId;

    @Column(name = "conflict_name", length = 100, nullable = false)
    private String conflictName;

    @Column(name = "danger_level", nullable = false)
    private Integer dangerLevel;

    @Column(name = "c_description", columnDefinition = "TEXT", nullable = false)
    private String cDescription;

    @Column(name = "solution", columnDefinition = "TEXT", nullable = false)
    private String solution;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;
}

