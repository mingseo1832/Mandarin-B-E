package mandarin.com.mandarin_backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "User")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // PK (자동 증가)

    // 🔹 공백 제외 최대 20자 (영문/숫자)
    @Column(nullable = false, unique = true, length = 20)
    @Pattern(regexp = "^[A-Za-z0-9]{1,20}$",
            message = "아이디는 공백 없이 영문/숫자 1~20자여야 합니다.")
    private String userId;

    // 🔹 한글만 허용 + 길이 1~5자
    @Column(nullable = false, length = 5)
    @Pattern(regexp = "^[가-힣]{1,5}$",
            message = "이름은 공백 없이 한글 1~5자여야 합니다.")
    private String username;

    // 🔹 공백 제외 최대 20자(영문/숫자 조합 가능)
    @Column(nullable = false, length = 20)
    @Pattern(regexp = "^[A-Za-z0-9!@#$%^&*]{1,20}$",
            message = "비밀번호는 공백 없이 1~20자여야 합니다.")
    private String password;

    // 러브타입 (ENFJ 등)
    @Column(length = 5)
    private String loveType;

    // 가입 시간
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(); // 자동 저장
    }
}