package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_detaillog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDetailLog {

    @Column(name = "conflict_id")
    private Integer conflictId;

    @Column(name = "sender", nullable = false)
    private String sender;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Id
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}

