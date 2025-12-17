package mandarin.com.mandarin_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mandarin.com.mandarin_backend.dto.KakaoTalkMessageDto;
import mandarin.com.mandarin_backend.dto.ParsedChatDataDto;
import mandarin.com.mandarin_backend.dto.ParsedDialogueDto;
import mandarin.com.mandarin_backend.util.KakaoTalkParser;
import mandarin.com.mandarin_backend.util.PiiMaskingUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 카카오톡 대화 파싱 및 전처리 서비스
 */
@Service
public class KakaoTalkParseService {

    private ObjectMapper objectMapper;

    /** 최대 문자 수 제한 기본값 */
    private static final int DEFAULT_MAX_CHARS = 150000;

    /**
     * 카카오톡 파일을 파싱하여 기본 정보 반환 (참여자 목록, 날짜 범위 등)
     */
    public ParsedChatDataDto parseInfo(MultipartFile file) throws IOException {
        String textContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        KakaoTalkParser parser = new KakaoTalkParser(textContent);
        return parser.getStatistics();
    }

    /**
     * 카카오톡 파일을 파싱하여 기본 정보 반환 (문자열 입력)
     */
    public ParsedChatDataDto parseInfo(String textContent) {
        KakaoTalkParser parser = new KakaoTalkParser(textContent);
        return parser.getStatistics();
    }

    // ============================================================
    // JSON 직렬화/역직렬화 메서드
    // ============================================================

    /**
     * 카카오톡 텍스트를 파싱하고 PII 마스킹 후 JSON 문자열로 변환 (DB 저장용)
     * 
     * @param textContent 카카오톡 대화 원본 텍스트
     * @return 파싱된 데이터의 JSON 문자열
     */
    public String parseAndConvertToJson(String textContent) {
        // 1. ObjectMapper 초기화 (없으면 생성)
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
        }
    
        // 2. 입력값이 JSON인지 확인 (기존 로직 유지)
        if (textContent.trim().startsWith("{")) {
            System.out.println("JSON 형식이 맞습니다.");
            try {
                ParsedDialogueDto dto = objectMapper.readValue(textContent, ParsedDialogueDto.class);
                return objectMapper.writeValueAsString(dto);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("JSON 변환 오류: " + e.getMessage(), e);
            }
        }
    
        // =========================================================
        // [문제 구간 수정] CASE 2: 텍스트 파싱
        // =========================================================
        
        // 1. 파서 실행
        KakaoTalkParser parser = new KakaoTalkParser(textContent);
        ParsedChatDataDto stats = parser.getStatistics(); // 통계 정보만 가져옴
    
        // ★ [디버깅 로그] 파싱이 제대로 됐는지 확인 (콘솔창 확인용)
        // parser.getDailyChats() 메서드가 없다면 KakaoTalkParser 클래스에 추가해야 합니다!
        Map<LocalDate, List<KakaoTalkMessageDto>> rawChats = parser.getDailyChats(); 
        
        if (rawChats == null || rawChats.isEmpty()) {
            System.err.println("🚨 경고: 파싱된 대화가 하나도 없습니다! 텍스트 형식을 확인하세요.");
            // 파싱된게 없으면 빈 JSON 리턴하지 말고 로그 확인
        } else {
            System.out.println("✅ 파싱 성공: " + rawChats.size() + "일치 대화 발견");
        }
    
        // 2. 메시지 내용 PII 마스킹
        Map<String, List<KakaoTalkMessageDto>> maskedDailyChats = new LinkedHashMap<>();
        
        // [수정 포인트] stats.getDailyChats() -> rawChats (파서에서 직접 가져온 데이터 사용)
        for (Map.Entry<LocalDate, List<KakaoTalkMessageDto>> entry : rawChats.entrySet()) {
            String dateKey = entry.getKey().toString(); 
            List<KakaoTalkMessageDto> maskedMessages = entry.getValue().stream()
                .map(msg -> KakaoTalkMessageDto.builder()
                    .sender(msg.getSender())
                    .time(msg.getTime())
                    .content(PiiMaskingUtil.mask(msg.getContent()))
                    .build())
                .collect(Collectors.toList());
            maskedDailyChats.put(dateKey, maskedMessages);
        }
        
        // 3. DTO 생성
        ParsedDialogueDto dto = ParsedDialogueDto.builder()
            .formatType(stats.getFormatType())
            .participants(stats.getParticipants())
            .dailyChats(maskedDailyChats) // 마스킹된 데이터 넣기
            .startDate(stats.getStartDate() != null ? stats.getStartDate().toString() : null)
            .endDate(stats.getEndDate() != null ? stats.getEndDate().toString() : null)
            .totalMessages(stats.getTotalMessages())
            .totalDays(stats.getTotalDays())
            .build();
        
        // 4. JSON 변환
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 변환 실패: " + e.getMessage(), e);
        }
    }

    /**
     * JSON 문자열을 ParsedDialogueDto로 역직렬화
     * 
     * @param json DB에서 가져온 JSON 문자열
     * @return 파싱된 대화 데이터
     */
    public ParsedDialogueDto parseJsonToDto(String json) {
        try {
            return objectMapper.readValue(json, ParsedDialogueDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 파싱 실패: " + e.getMessage(), e);
        }
    }

    /**
     * JSON에서 특정 날짜 기준으로 데이터 필터링 (재파싱 없음)
     * DB에 저장된 JSON 문자열에서 targetDate, bufferDays를 기준으로 필터링
     * 
     * @param json DB에서 가져온 JSON 문자열
     * @param targetName 분석 대상 인물 이름
     * @param targetDate 기준 날짜 (null이면 가장 최신 날짜)
     * @param bufferDays 기준 날짜 이전 버퍼 일수
     * @param maxChars 최대 문자 수 제한
     * @return 필터링된 결과
     */
    public PreprocessResult filterFromJson(
            String json,
            String targetName,
            LocalDate targetDate,
            int bufferDays,
            int maxChars) {
        
        // 1. JSON 역직렬화
        ParsedDialogueDto dto = parseJsonToDto(json);
        
        // 2. targetDate가 null이면 가장 최신 날짜 사용
        LocalDate effectiveTargetDate = targetDate;
        if (effectiveTargetDate == null && dto.getEndDate() != null) {
            effectiveTargetDate = LocalDate.parse(dto.getEndDate());
        }
        
        // 3. 날짜 범위 계산
        LocalDate startDate = effectiveTargetDate != null 
            ? effectiveTargetDate.minusDays(bufferDays) 
            : null;
        
        // 4. 날짜별 필터링
        Map<String, List<KakaoTalkMessageDto>> filteredChats = new LinkedHashMap<>();
        int targetMessageCount = 0;
        boolean targetFound = false;
        
        for (Map.Entry<String, List<KakaoTalkMessageDto>> entry : dto.getDailyChats().entrySet()) {
            LocalDate chatDate = LocalDate.parse(entry.getKey());
            
            // 날짜 범위 체크
            if (startDate != null && effectiveTargetDate != null) {
                if (chatDate.isBefore(startDate) || chatDate.isAfter(effectiveTargetDate)) {
                    continue;
                }
            }
            
            filteredChats.put(entry.getKey(), entry.getValue());
            
            // 타겟 인물 메시지 카운트
            for (KakaoTalkMessageDto msg : entry.getValue()) {
                if (msg.getSender() != null && msg.getSender().contains(targetName)) {
                    targetFound = true;
                    targetMessageCount++;
                }
            }
        }
        
        // 5. 텍스트로 변환
        StringBuilder resultText = new StringBuilder();
        for (Map.Entry<String, List<KakaoTalkMessageDto>> entry : filteredChats.entrySet()) {
            LocalDate date = LocalDate.parse(entry.getKey());
            resultText.append(String.format("--- %d년 %02d월 %02d일 ---\n",
                date.getYear(), date.getMonthValue(), date.getDayOfMonth()));
            
            for (KakaoTalkMessageDto msg : entry.getValue()) {
                if (msg.getSender() != null) {
                    resultText.append(String.format("[%s] [%s] %s\n",
                        msg.getSender(), msg.getTime(), msg.getContent()));
                } else {
                    resultText.append(msg.getContent()).append("\n");
                }
            }
        }
        
        String text = resultText.toString();
        
        // 6. 최대 길이 제한
        if (text.length() > maxChars) {
            text = text.substring(text.length() - maxChars);
        }
        
        // 7. 통계 정보 생성
        ParsedChatDataDto stats = ParsedChatDataDto.builder()
            .formatType(dto.getFormatType())
            .participants(dto.getParticipants())
            .totalDays(filteredChats.size())
            .totalMessages(filteredChats.values().stream().mapToInt(List::size).sum())
            .startDate(dto.getStartDate() != null ? LocalDate.parse(dto.getStartDate()) : null)
            .endDate(dto.getEndDate() != null ? LocalDate.parse(dto.getEndDate()) : null)
            .build();
        
        return PreprocessResult.builder()
            .text(text)
            .stats(stats)
            .targetFound(targetFound)
            .targetMessageCount(targetMessageCount)
            .filteredCharCount(text.length())
            .build();
    }
    

    /**
     * 특정 날짜 기준으로 bufferDays 이전까지의 대화 필터링 (페르소나 분석용)
     * 원본 텍스트를 파싱하여 targetDate, bufferDays 기준으로 필터링
     * 
     * @param textContent 카카오톡 대화 텍스트 (DB에서 가져온 fullDialogue)
     * @param targetName 분석 대상 인물 이름
     * @param targetDate 기준 날짜 (null이면 가장 최신 날짜)
     * @param bufferDays 기준 날짜 이전 버퍼 일수
     * @param maxChars 최대 문자 수 제한
     * @return 필터링된 대화 텍스트와 통계 정보
     */
    public PreprocessResult preprocessByTargetDate(
            String textContent,
            String targetName,
            LocalDate targetDate,
            int bufferDays,
            int maxChars) {
        
        KakaoTalkParser parser = new KakaoTalkParser(textContent);
        ParsedChatDataDto stats = parser.getStatistics();

        // 1. 특정 날짜 기준 필터링
        Map<LocalDate, List<KakaoTalkMessageDto>> filteredData = 
            parser.filterByTargetDate(targetDate, bufferDays);

        // 2. 타겟 인물이 해당 기간에 존재하는지 확인
        Map<LocalDate, List<KakaoTalkMessageDto>> targetMessages = 
            parser.filterBySender(targetName, filteredData);
        
        boolean targetFound = !targetMessages.isEmpty();
        int targetMessageCount = targetMessages.values().stream()
            .mapToInt(List::size)
            .sum();

        // 3. 전체 대화 텍스트 변환
        String resultText = parser.toText(filteredData);

        // 4. 최대 길이 제한 (최신 대화 우선)
        if (resultText.length() > maxChars) {
            resultText = resultText.substring(resultText.length() - maxChars);
        }

        return PreprocessResult.builder()
            .text(resultText)
            .stats(stats)
            .targetFound(targetFound)
            .targetMessageCount(targetMessageCount)
            .filteredCharCount(resultText.length())
            .build();
    }

    /**
     * JSON 문자열을 텍스트로 변환 (가장 최근부터 maxChars만큼)
     * 
     * @param json DB에서 가져온 JSON 문자열
     * @param maxChars 최대 문자 수 제한
     * @return 변환된 대화 텍스트 (가장 최근부터 maxChars만큼)
     */
    public String convertJsonToText(String json, int maxChars) {
        // 1. JSON 역직렬화
        ParsedDialogueDto dto = parseJsonToDto(json);
        
        // 2. 원본 순서대로 메시지 리스트 생성(오래된 날짜 -> 최신 날짜)
        List<KakaoTalkMessageDto> allMessages = new ArrayList<>();
        for (List<KakaoTalkMessageDto> messages : dto.getDailyChats().values()) {
            allMessages.addAll(messages);
        }
        
        // 3. 텍스트로 변환 (최신 메시지부터)
        StringBuilder resultText = new StringBuilder();
        for (KakaoTalkMessageDto msg : allMessages) {
            if (msg.getSender() != null) {
                resultText.append(String.format("[%s] [%s] %s\n",
                    msg.getSender(), msg.getTime(), msg.getContent()));
            } else {
                resultText.append(msg.getContent()).append("\n");
            }
        }
        
        String text = resultText.toString();
        
        // 4. 가장 최근부터 maxChars만큼만 추출
        if (text.length() > maxChars) {
            text = text.substring(text.length() - maxChars);
        }
        
        return text;
    }

    /**
     * 전처리 결과 클래스
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PreprocessResult {
        private String text;
        private ParsedChatDataDto stats;
        private boolean targetFound;
        private int targetMessageCount;
        private int filteredCharCount;
    }

    public static int getDefaultMaxChars() {
        return DEFAULT_MAX_CHARS;
    }
}

