package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;  // 보고서 PK

    @ManyToOne
    @JoinColumn(name = "simulation_id", nullable = false)
    private Simulation simulation;  // 어떤 시뮬레이션에 대한 분석인지

    @ManyToOne
    @JoinColumn(name = "character_id", nullable = false)
    private User_Character character;  // 대상 캐릭터

    @Column(nullable = false, columnDefinition = "TEXT")
    private String analysisSummary;   // 분석 요약

    @Column(nullable = false, columnDefinition = "JSON")
    private String suggestedActions;  // JSON 형태의 조언 리스트

    @Column(columnDefinition = "JSON")
    private String visualDataJson;    // 그래프/시각화 데이터

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 생성 시간

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}