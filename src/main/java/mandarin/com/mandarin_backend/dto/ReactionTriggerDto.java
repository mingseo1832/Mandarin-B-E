package mandarin.com.mandarin_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 상대방의 특정 말에 대한 반응 트리거 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionTriggerDto {
    
    /**
     * 상대방(사용자)이 한 말/행동/주제
     * 예: '칭찬', '약속 변경', '애정 표현'
     */
    private String trigger;
    
    /**
     * 그에 대한 이 사람의 반응 패턴
     * 예: '기분 좋아하며 이모티콘 많이 사용', '짜증내며 단답으로 변함'
     */
    private String reaction;
    
    /**
     * 실제 대화에서 해당 반응이 나타난 예시 문장
     */
    private String example;
}
