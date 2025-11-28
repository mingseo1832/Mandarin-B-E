package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Simulation_Message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Simulation_Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;     // 메시지 PK

    @ManyToOne
    @JoinColumn(name = "simulation_id", nullable = false)
    private Simulation simulation;  // 어떤 시뮬레이션의 메시지인지

    @Column(nullable = false)
    private Boolean sender;     // 보낸 사람 (false = 유저, true = 캐릭터)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;     // 메시지 내용

    @Column(nullable = false)
    private LocalDateTime timestamp;   // 메시지 전송 시간
}
