package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import mandarin.com.mandarin_backend.entity.enums.SimulationCategory;
import mandarin.com.mandarin_backend.entity.enums.SimulationPurpose;
import java.time.LocalDateTime;

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

    /**
     * FK → User
     */
    @ManyToOne
    @JoinColumn(name = "id", nullable = false)
    private User user;

    /**
     * FK → UserCharacter
     */
    @ManyToOne
    @JoinColumn(name = "character_id", nullable = false)
    private UserCharacter character;

    @Column(name = "simulation_name", length = 50, nullable = false)
    private String simulationName;   // 시뮬레이션 이름

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false)
    private SimulationPurpose purpose;   // FUTURE / PAST
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private SimulationCategory category; // 10가지 ENUM 카테고리
    
    @Column(name = "time", nullable = false)
    private LocalDateTime time; // 생성 시간 or 실행 시간

    @Column(name = "last_update_time", nullable = false)
    private LocalDateTime lastUpdateTime; // 마지막 수정 시간

    @Column(name = "is_finished", nullable = false)
    private Boolean isFinished; // 시뮬레이션 종료 여부

    @Column(name = "few_shot_context", columnDefinition = "LONGTEXT", nullable = false)
    private String fewShotContext; // Few-shot 문맥

    @Column(name = "character_persona", columnDefinition = "LONGTEXT", nullable = false)
    private String characterPersona; // 캐릭터 페르소나

    /**
     * 기본값 세팅
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.time == null) {
            this.time = now;
        }
        if (this.lastUpdateTime == null) {
            this.lastUpdateTime = now;
        }
        if (this.isFinished == null) {
            this.isFinished = false;
        }
    }
}
