package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;
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

    public enum SimulationPurpose {
        FUTURE,
        PAST
    }
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private SimulationCategory category; // 10가지 ENUM 카테고리

    public enum SimulationCategory {
        EMOTIONAL_MISTAKE,
        MISCOMMUNICATION,
        CONTACT_ISSUE,
        BREAKUP_PROCESS,
        REALITY_PROBLEM,
        RELATION_TENSION,
        PERSONAL_BOUNDARY,
        FAMILY_FRIEND_ISSUE,
        BREAKUP_FUTURE,
        EVENT_PREPARATION
    }
    
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
}
