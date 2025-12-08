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
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String characterName;   // 캐릭터 이름

    @Column(nullable = false)
    private int characterAge;       // 캐릭터 나이

    @Column(nullable = false)
    private int relationType;       // 관계 타입 코드값

    @Column(length = 255)
    private String characterImg;    // 캐릭터 이미지 URL

    private LocalDateTime meetDate; // 만난 날짜

    /**
     * loveType: 캐릭터의 러브타입 INT 값
     * - Lovetype 테이블 삭제로 인해 FK 제거됨
     * - 0~15 : 러브타입 코드
     * - 16   : 미설정(default)
     */
    @Column(name = "love_type", nullable = false)
    @Builder.Default
    private Integer loveType = 16;

    @Column(name = "kakao_name", nullable = false, length = 50)
    private String kakaoName;  // 카카오톡에서의 이름 (targetName)

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String fullDialogue;    // 전체 대화 내용 저장

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 생성 시간

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

