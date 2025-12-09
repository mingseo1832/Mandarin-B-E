package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import mandarin.com.mandarin_backend.entity.enums.SimulationCategory;
import mandarin.com.mandarin_backend.entity.enums.SimulationPurpose;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "Simulation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Simulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "simulation_id")
    private Long simulationId;  // PK

    @ManyToOne
    @JoinColumn(name = "character_id", nullable = false)
    private UserCharacter character;  // 어떤 캐릭터와의 시뮬레이션인지

    @Column(length = 50, nullable = false)
    private String simulationName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SimulationPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SimulationCategory category;

    @Column(nullable = false)
    private LocalDateTime time;

    @Column(nullable = false)
    private LocalDateTime lastUpdateTime;

    @Column(nullable = false)
    @ColumnDefault("false") // DB 레벨에서의 Default
    @Builder.Default        // Java Builder 패턴 사용 시 Default
    private Boolean isFinished = false;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String fewShotContext;

    @Column(columnDefinition = "json", nullable = false)
    private String characterPersona;

    // --- 자동 시간 설정 로직 ---

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.time = now;
        this.lastUpdateTime = now;
        if (this.isFinished == null) {
            this.isFinished = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.lastUpdateTime = LocalDateTime.now();
    }
}
