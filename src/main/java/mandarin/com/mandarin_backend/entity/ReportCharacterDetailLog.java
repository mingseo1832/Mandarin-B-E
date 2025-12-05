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

    @Column(name = "chat_report_id")
    private Integer chatReportId;

    @Column(name = "sender", nullable = false)
    private String sender;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Id
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}

