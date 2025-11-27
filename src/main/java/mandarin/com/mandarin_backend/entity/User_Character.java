package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "User_Character")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User_Character {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long characterId;   // 캐릭터 PK

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;   // User 엔티티와 연결 (FK)

    @Column(nullable = false, length = 50)
    private String characterName;   // 캐릭터 이름

    @Column(nullable = false)
    private int characterAge;       // 캐릭터 나이

    @Column(nullable = false)
    private int relationType;       // 관계 타입 (ex. 전 연인, 현재 썸 등)

    @Column(length = 255)
    private String characterImg;    // 캐릭터 이미지 URL

    private LocalDateTime meetDate; // 만난 날짜

    @ManyToOne
    @JoinColumn(name = "love_type")
    private LoveType loveType;      // 러브타입 FK (러브타입 분석 결과)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;          // AI 캐릭터 생성용 프롬프트

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String fullDialogue;    // 전체 대화 내용 저장

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String fewShotContext;  // few-shot 문맥 저장

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 생성 시간

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();   // 자동 생성
    }
}
