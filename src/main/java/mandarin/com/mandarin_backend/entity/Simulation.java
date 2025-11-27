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
    private Long simulationId;  // 시뮬레이션 PK

    @ManyToOne
    @JoinColumn(name = "character_id", nullable = false)
    private User_Character character;  // 어떤 캐릭터와의 시뮬레이션인지

    @Column(nullable = false)
    private LocalDateTime startTime;   // 시작 시간

    private LocalDateTime endTime;     // 종료 시간

    @Column(length = 100)
    private String initUserMood;       // 사용자의 초기 감정 상태
}
