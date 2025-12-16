package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_report_detaillog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReportDetailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_report_detaillog_id")
    private Long chatReportDetailLogId;

    @Column(name = "chat_report_id")
    private Integer chatReportId;

    /**
     * sender: 메시지 발신자
     * - "user" = 사용자
     * - "assistant" = AI (캐릭터)
     */
    @Column(name = "sender", nullable = false)
    private String sender;

    /**
     * message_simulation: 점수 산정에 영향을 준 주요 대화 내용 (KeyConversation)
     */
    @Column(name = "message_simulation", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}

