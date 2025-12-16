package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_character_detaillog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCharacterDetailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_character_detaillog_id")
    private Long reportCharacterDetailLogId;  // PK

    /**
     * sender: 메시지 발신자
     * - "user" = 사용자
     * - "character" = 캐릭터 (분석 대상)
     */
    @Column(name = "sender", nullable = false, length = 20)
    private String sender;

    @Column(name = "message_kakao", columnDefinition = "TEXT", nullable = false)
    private String messageKakao;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * FK → ReportCharacter
     */
    @ManyToOne
    @JoinColumn(name = "report_character_id", nullable = false)
    private ReportCharacter reportCharacter;
}
