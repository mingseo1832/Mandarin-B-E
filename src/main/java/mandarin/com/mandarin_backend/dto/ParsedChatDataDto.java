package mandarin.com.mandarin_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 카카오톡 파싱 결과 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedChatDataDto {
    
    private String formatType;                              // 파일 형식 (windows/mac/ios/android)
    private int totalDays;                                  // 총 대화 일수
    private int totalMessages;                              // 총 메시지 수
    private List<String> participants;                      // 참여자 목록
    private int participantCount;                           // 참여자 수
    private LocalDate startDate;                            // 대화 시작일
    private LocalDate endDate;                              // 대화 종료일
    private Map<LocalDate, List<KakaoTalkMessageDto>> dailyChats;  // 날짜별 메시지
}

