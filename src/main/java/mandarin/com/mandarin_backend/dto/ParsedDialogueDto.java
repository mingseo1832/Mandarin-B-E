package mandarin.com.mandarin_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 파싱된 대화 데이터를 JSON으로 저장하기 위한 DTO
 * 
 * DB의 fullDialogue 필드에 JSON 문자열로 저장되며,
 * 이후 사용 시 재파싱 없이 바로 사용 가능
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedDialogueDto {
    
    // 파일 형식 (windows/mac/ios/android)
    private String formatType;
    
    // 참여자 목록
    private List<String> participants;
    
    // 날짜별 메시지 (key: "2025-01-15" 형식의 날짜 문자열)
    private Map<String, List<KakaoTalkMessageDto>> dailyChats;
    
    // 대화 시작일 (YYYY-MM-DD)
    private String startDate;
    
    // 대화 종료일 (YYYY-MM-DD)
    private String endDate;
    
    // 총 메시지 수
    private int totalMessages;
    
    // 총 대화 일수
    private int totalDays;
}

