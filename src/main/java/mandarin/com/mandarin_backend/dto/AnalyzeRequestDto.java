package mandarin.com.mandarin_backend.dto;

import lombok.Data;

@Data
public class AnalyzeRequestDto {
    private String textContent;
    private String targetName;
    
    // 기간 지정 옵션 1: 최근 N일 (기본값 14일)
    private Integer periodDays = 14;
    
    // 기간 지정 옵션 2: 시작일~종료일 (YYYY-MM-DD 형식)
    // startDate와 endDate가 모두 지정되면 periodDays보다 우선
    private String startDate;
    private String endDate;
    
    // 시작일 이전 버퍼 일수 (기본값 7일, 맥락 파악용)
    private Integer bufferDays = 7;
}
