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
     * 갈등 상황을 한 단어로 표현한 키워드
     * 예: '비난', '무관심', '짜증', '비아냥', '회피', '무시'
     */
    private String keyword;
    
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
     * 갈등이 발생한 구체적인 원인 분석
     * 예: '칭찬을 하지 않아 상대방이 불편함을 느끼고 갈등 상황이 발생함'
     */
    private String cause;

    /**
     * 갈등 상황을 해결하기 위한 구체적인 조언
     * 예: '칭찬을 하지 않아 상대방이 불편함을 느끼고 갈등 상황이 발생함'
     */
    private String solution;
    
    /**
     * 실제 대화에서 해당 반응이 나타난 예시 문장
     */
    private String example;

    /**
     * 이 갈등 요소의 위험도 점수 (0~100)
     * AI가 산정한 위험도
     */
    private Integer dangerLevel;
}
