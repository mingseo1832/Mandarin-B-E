package mandarin.com.mandarin_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "LoveType")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ltId;   // 러브타입 PK

    @Column(nullable = false, length = 20)
    private String ltName;   // 러브타입 이름 (EX: ENFJ, ESTP)

    @Column(nullable = false, length = 255)
    private String ltImg;    // 러브타입 이미지 URL

    @Column(nullable = false, length = 255)
    private String ltDesc;   // 설명 텍스트

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;   // 생성 시간

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();   // DB 저장 시 자동 설정
    }
}
