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
public class UserCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long characterId;   // 캐릭터 PK

    // 기존 ManyToOne 유지 (캐릭터는 반드시 User에 속함)
    @ManyToOne
    @JoinColumn(name = "id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String characterName;   // 캐릭터 이름

    @Column(nullable = false)
    private int characterAge;       // 캐릭터 나이

    /**
     * relationType : 채팅 캐릭터와의 관계 유형
     * - 0 : 썸
     * - 1 : 연애 중
     * - 2 : 결별
     */
    @Column(nullable = false)
    private int relationType;

    @Column(length = 255)
    private String characterImg;    // 캐릭터 이미지 URL

    private LocalDateTime meetDate; // 만난 날짜

    @Column(name = "kakao_name", nullable = false, length = 50)
    private String kakaoName; // 카카오톡 이름

    /**
     * loveType: 캐릭터의 러브타입 INT 값
     * - Lovetype 테이블 삭제로 인해 FK 제거됨
     * - 0~15 : 러브타입 코드
     * - 16   : 미설정(default)
     */
    @Column(name = "c_lovetype", nullable = false)
    @Builder.Default
    private Integer loveType = 16;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String fullDialogue;    // 전체 대화 내용 저장

    @Column(nullable = false, columnDefinition = "TEXT")
    private String historySum;      // 캐릭터의 과거 대화 요약 데이터

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 생성 시간

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
