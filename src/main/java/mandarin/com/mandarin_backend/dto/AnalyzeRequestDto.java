package mandarin.com.mandarin_backend.dto;

import lombok.Data;

/**
 * 페르소나 분석 및 시뮬레이션 생성 요청 DTO
 * 
 * DB에 저장된 fullDialogue에서 특정 날짜 기준으로 데이터를 필터링하여
 * 상대방(kakaoName 제외) 페르소나 추출 후 Simulation에 저장
 */
@Data
public class AnalyzeRequestDto {

    // ===== 사용자 정보 =====
    
    // 사용자 ID
    private String userId;
    
    // ===== 캐릭터 정보 =====
    
    // DB에서 fullDialogue를 조회할 캐릭터 ID
    private Long characterId;
    
    // ===== 날짜 필터링 =====
    
    // 기준 날짜 (YYYY-MM-DD 형식)
    // 이 날짜로부터 bufferDays 이전까지의 데이터에서 페르소나 추출
    private String targetDate;
    
    // 기준 날짜 이전 버퍼 일수 (기본값 7일)
    // targetDate가 2025-01-15이고 bufferDays가 7이면 2025-01-08 ~ 2025-01-15 데이터 사용
    private Integer bufferDays = 7;
    
    // ===== 시뮬레이션 정보 =====
    
    // 시뮬레이션 이름
    private String simulationName;
    
    // 시뮬레이션 목적 (FUTURE: 미래 시뮬레이션, PAST: 과거 후회 시뮬레이션)
    private String purpose;
    
    // 시뮬레이션 카테고리
    private String category;
}
