package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ReportSummary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report_Summary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportSummaryId;  // 요약 보고서 PK
}
