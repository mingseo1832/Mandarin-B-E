package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Chat_Report_DetailLog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReportDetailLog {

    @Id
    @Column(name = "Chat_Report_DetailLog_id")
    private Long chatReportDetailLogId;

    @Column(name = "chat_report_id")
    private Integer chatReportId;

    @Column(name = "sender", nullable = false)
    private String sender;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}

