package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Report_Character_DetailLog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCharacterDetailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Report_Character_DetailLog_id")
    private Long reportCharacterDetailLogId;  // PK

    /**
     * sender:
     * false → USER
     * true  → CHARACTER
     */
    @Column(name = "sender", nullable = false)
    private Boolean sender;

    @Column(name = "message_kakao", columnDefinition = "TEXT", nullable = false)
    private String messageKakao;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * FK → ReportCharacter
     */
    @ManyToOne
    @JoinColumn(name = "Report_Character_id", nullable = false)
    private ReportCharacter reportCharacter;
}
