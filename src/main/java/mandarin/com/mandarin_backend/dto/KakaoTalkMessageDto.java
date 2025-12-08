package mandarin.com.mandarin_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 카카오톡 개별 메시지 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KakaoTalkMessageDto {
    
    private String sender;      // 발신자 이름
    private String time;        // 시간 문자열 (오전/오후 HH:mm)
    private String content;     // 메시지 내용
}

