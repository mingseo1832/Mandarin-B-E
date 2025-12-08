package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Report_Character")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_character_id")
    private Integer reportCharacterId;  // PK

    /**
     * FK → ChatReport (어떤 레포트의 캐릭터 분석인지)
     */
    @ManyToOne
    @JoinColumn(name = "chat_report_id", nullable = false)
    private ChatReport chatReport;

    /**
     * FK → UserCharacter
     */
    @ManyToOne
    @JoinColumn(name = "character_id")
    private UserCharacter character;

    /**
     * 분석된 갈등 요소 이름
     */
    @Column(name = "conflict_name", nullable = false, length = 100)
    private String conflictName;

    /**
     * 위험도 0~100
     */
    @Column(name = "danger_level", nullable = false)
    private Integer dangerLevel;

    /**
     * 갈등에 대한 설명
     */
    @Column(name = "c_description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * 해결 방안
     */
    @Column(name = "solution", nullable = false, columnDefinition = "TEXT")
    private String solution;

    /**
     * 행동 가이드 이름
     */
    @Column(name = "action_name", nullable = false, length = 100)
    private String actionName;

    /**
     * 행동 가이드 설명
     */
    @Column(name = "act_description", nullable = false, columnDefinition = "TEXT")
    private String actionDescription;

    /**
     * 행동 항목 활성화 여부
     */
    @Column(name = "check_active", nullable = false)
    private Boolean checkActive;
}
