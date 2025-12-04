package mandarin.com.mandarin_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 점수 산정에 영향을 준 주요 대화
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyConversationDto {
    
    private String role;     // "user" 또는 "assistant"
    private String content;  // 메시지 내용
}

