package mandarin.com.mandarin_backend.util;

import mandarin.com.mandarin_backend.dto.KakaoTalkMessageDto;
import mandarin.com.mandarin_backend.dto.ParsedChatDataDto;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 카카오톡 대화 파싱 및 날짜별 청킹 클래스
 * 
 * 지원 플랫폼:
 * - Windows: [이름] [시간] 메시지 형식
 * - Mac: CSV 파일 (Date,User,Message)
 * - iOS: 2025. 10. 23. 오전 11:44, 이름 : 메시지 형식
 * - Android: 2025년 10월 10일 오전 10:09, 이름 : 메시지 형식
 */
public class KakaoTalkParser {

    // ===== 윈도우 패턴 =====
    // 날짜: --------------- 2025년 8월 14일 목요일 ---------------
    private static final Pattern WINDOWS_DATE_PATTERN = 
        Pattern.compile("^-+ (\\d{4})년 (\\d{1,2})월 (\\d{1,2})일.*-+$");
    // 메시지: [이재균] [오전 12:01] 사진
    private static final Pattern WINDOWS_MESSAGE_PATTERN = 
        Pattern.compile("^\\[([^\\]]+)\\]\\s*\\[([^\\]]+)\\]\\s*(.*)$");

    // ===== iOS 패턴 =====
    // 날짜: 2025년 10월 23일 목요일
    private static final Pattern IOS_DATE_PATTERN = 
        Pattern.compile("^(\\d{4})년 (\\d{1,2})월 (\\d{1,2})일 \\S+요일\\s*$");
    // 메시지: 2025. 10. 23. 오전 11:44, 안도현 : 동방에 다 있을걸
    private static final Pattern IOS_MESSAGE_PATTERN = 
        Pattern.compile("^(\\d{4})\\. (\\d{1,2})\\. (\\d{1,2})\\. (오전|오후) (\\d{1,2}):(\\d{2}),\\s*([^:]+)\\s*:\\s*(.*)$");

    // ===== 안드로이드 패턴 =====
    // 메시지: 2025년 10월 10일 오전 10:09, 이재균 : ㅎㅇ
    private static final Pattern ANDROID_MESSAGE_PATTERN = 
        Pattern.compile("^(\\d{4})년 (\\d{1,2})월 (\\d{1,2})일 (오전|오후) (\\d{1,2}):(\\d{2}),\\s*([^:]+)\\s*:\\s*(.*)$");

    // ===== 맥 CSV 패턴 =====
    private static final String MAC_CSV_HEADER = "Date,User,Message";
    private static final DateTimeFormatter MAC_DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String rawText;
    private final Map<LocalDate, List<KakaoTalkMessageDto>> dailyChats;
    private String formatType;

    public KakaoTalkParser(String textContent) {
        this.rawText = textContent;
        this.dailyChats = new LinkedHashMap<>();
        this.formatType = "unknown";
        detectFormat();
        parse();
    }

    /**
     * 파일 형식 자동 감지
     */
    private void detectFormat() {
        String[] lines = rawText.split("\n");
        int checkLimit = Math.min(20, lines.length);

        for (int i = 0; i < checkLimit; i++) {
            String line = lines[i].trim();

            // 맥 CSV 감지
            if (line.equals(MAC_CSV_HEADER) || line.startsWith("Date,User,")) {
                this.formatType = "mac";
                return;
            }

            // 윈도우 감지
            if (WINDOWS_DATE_PATTERN.matcher(line).matches()) {
                this.formatType = "windows";
                return;
            }

            // iOS 감지
            if (IOS_MESSAGE_PATTERN.matcher(line).matches()) {
                this.formatType = "ios";
                return;
            }

            // 안드로이드 감지
            if (ANDROID_MESSAGE_PATTERN.matcher(line).matches()) {
                this.formatType = "android";
                return;
            }
        }

        // 기본값은 윈도우
        this.formatType = "windows";
    }

    /**
     * 전체 텍스트를 파싱하여 날짜별로 구조화
     */
    private void parse() {
        switch (formatType) {
            case "mac":
                parseMac();
                break;
            case "ios":
                parseIos();
                break;
            case "android":
                parseAndroid();
                break;
            default:
                parseWindows();
        }
    }

    /**
     * 오전/오후를 24시간 형식으로 변환
     */
    private int convertAmPmHour(String ampm, int hour) {
        if ("오후".equals(ampm) && hour != 12) {
            return hour + 12;
        } else if ("오전".equals(ampm) && hour == 12) {
            return 0;
        }
        return hour;
    }

    /**
     * 시간을 오전/오후 형식 문자열로 변환
     */
    private String formatTimeStr(int hour, int minute) {
        String ampm = hour < 12 ? "오전" : "오후";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format("%s %d:%02d", ampm, displayHour, minute);
    }

    /**
     * 메시지를 dailyChats에 추가
     */
    private void addMessage(LocalDate date, String sender, String timeStr, String content) {
        dailyChats.computeIfAbsent(date, k -> new ArrayList<>())
            .add(KakaoTalkMessageDto.builder()
                .sender(sender.trim())
                .time(timeStr)
                .content(content.trim())
                .build());
    }

    /**
     * 윈도우 버전 파싱: [이름] [시간] 메시지
     */
    private void parseWindows() {
        String[] lines = rawText.split("\n");
        LocalDate currentDate = null;
        List<KakaoTalkMessageDto> currentMessages = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.trim();

            // 날짜 라인 체크
            Matcher dateMatcher = WINDOWS_DATE_PATTERN.matcher(line);
            if (dateMatcher.matches()) {
                // 이전 날짜 데이터 저장
                if (currentDate != null && !currentMessages.isEmpty()) {
                    dailyChats.put(currentDate, new ArrayList<>(currentMessages));
                }

                int year = Integer.parseInt(dateMatcher.group(1));
                int month = Integer.parseInt(dateMatcher.group(2));
                int day = Integer.parseInt(dateMatcher.group(3));
                currentDate = LocalDate.of(year, month, day);
                currentMessages = new ArrayList<>();
                continue;
            }

            // 메시지 파싱
            if (currentDate != null) {
                Matcher msgMatcher = WINDOWS_MESSAGE_PATTERN.matcher(line);
                if (msgMatcher.matches()) {
                    currentMessages.add(KakaoTalkMessageDto.builder()
                        .sender(msgMatcher.group(1))
                        .time(msgMatcher.group(2))
                        .content(msgMatcher.group(3))
                        .build());
                } else if (!line.isEmpty() && !line.startsWith("메시지가 삭제")) {
                    // 연속 메시지 또는 시스템 메시지
                    currentMessages.add(KakaoTalkMessageDto.builder()
                        .sender(null)
                        .time(null)
                        .content(line)
                        .build());
                }
            }
        }

        // 마지막 날짜 저장
        if (currentDate != null && !currentMessages.isEmpty()) {
            dailyChats.put(currentDate, currentMessages);
        }
    }

    /**
     * 맥 CSV 버전 파싱: Date,User,Message
     */
    private void parseMac() {
        try (BufferedReader reader = new BufferedReader(new StringReader(rawText))) {
            String headerLine = reader.readLine(); // 헤더 스킵
            if (headerLine == null) return;

            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    // 간단한 CSV 파싱 (따옴표 처리)
                    String[] parts = parseCSVLine(line);
                    if (parts.length < 3) continue;

                    String dateStr = parts[0];
                    String user = parts[1];
                    String message = parts[2];

                    if (dateStr.isEmpty() || user.isEmpty()) continue;

                    LocalDateTime parsedDt = LocalDateTime.parse(dateStr, MAC_DATE_FORMATTER);
                    String timeStr = formatTimeStr(parsedDt.getHour(), parsedDt.getMinute());

                    addMessage(parsedDt.toLocalDate(), user, timeStr, message);
                } catch (Exception e) {
                    // 파싱 실패 시 무시
                }
            }
        } catch (Exception e) {
            // CSV 파싱 실패 시 빈 상태 유지
        }
    }

    /**
     * 간단한 CSV 라인 파싱 (따옴표 처리)
     */
    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());

        return result.toArray(new String[0]);
    }

    /**
     * iOS 버전 파싱: 2025. 10. 23. 오전 11:44, 안도현 : 메시지
     * 날짜 라인: 2025년 10월 23일 목요일
     */
    private void parseIos() {
        String[] lines = rawText.split("\n");
        LocalDate currentDate = null;

        for (String rawLine : lines) {
            String line = rawLine.trim();

            // 날짜 라인 체크 (예: "2025년 10월 23일 목요일")
            Matcher dateMatcher = IOS_DATE_PATTERN.matcher(line);
            if (dateMatcher.matches()) {
                try {
                    int year = Integer.parseInt(dateMatcher.group(1));
                    int month = Integer.parseInt(dateMatcher.group(2));
                    int day = Integer.parseInt(dateMatcher.group(3));
                    currentDate = LocalDate.of(year, month, day);
                    continue;
                } catch (Exception e) {
                    // 날짜 파싱 실패 시 무시
                }
            }

            // 메시지 파싱
            Matcher msgMatcher = IOS_MESSAGE_PATTERN.matcher(line);
            if (msgMatcher.matches()) {
                try {
                    int year = Integer.parseInt(msgMatcher.group(1));
                    int month = Integer.parseInt(msgMatcher.group(2));
                    int day = Integer.parseInt(msgMatcher.group(3));
                    String ampm = msgMatcher.group(4);
                    int hour = Integer.parseInt(msgMatcher.group(5));
                    int minute = Integer.parseInt(msgMatcher.group(6));
                    String sender = msgMatcher.group(7);
                    String content = msgMatcher.group(8);

                    int hour24 = convertAmPmHour(ampm, hour);
                    LocalDate date = LocalDate.of(year, month, day);
                    
                    // 날짜 라인이 있으면 그것을 우선 사용, 없으면 메시지의 날짜 사용
                    if (currentDate != null) {
                        date = currentDate;
                    }
                    
                    String timeStr = formatTimeStr(hour24, minute);
                    addMessage(date, sender, timeStr, content);
                } catch (Exception e) {
                    // 파싱 실패 시 무시
                }
            }
        }
    }

    /**
     * 안드로이드 버전 파싱: 2025년 10월 10일 오전 10:09, 이재균 : 메시지
     */
    private void parseAndroid() {
        String[] lines = rawText.split("\n");

        for (String rawLine : lines) {
            String line = rawLine.trim();

            Matcher msgMatcher = ANDROID_MESSAGE_PATTERN.matcher(line);
            if (msgMatcher.matches()) {
                try {
                    int year = Integer.parseInt(msgMatcher.group(1));
                    int month = Integer.parseInt(msgMatcher.group(2));
                    int day = Integer.parseInt(msgMatcher.group(3));
                    String ampm = msgMatcher.group(4);
                    int hour = Integer.parseInt(msgMatcher.group(5));
                    int minute = Integer.parseInt(msgMatcher.group(6));
                    String sender = msgMatcher.group(7);
                    String content = msgMatcher.group(8);

                    int hour24 = convertAmPmHour(ampm, hour);
                    LocalDate date = LocalDate.of(year, month, day);
                    String timeStr = formatTimeStr(hour24, minute);

                    addMessage(date, sender, timeStr, content);
                } catch (Exception e) {
                    // 파싱 실패 시 무시
                }
            }
        }
    }

    // ===== Public 메서드 =====

    /**
     * 대화의 시작일과 종료일 반환
     */
    public LocalDate[] getDateRange() {
        if (dailyChats.isEmpty()) {
            return new LocalDate[]{null, null};
        }
        List<LocalDate> dates = new ArrayList<>(dailyChats.keySet());
        return new LocalDate[]{
            Collections.min(dates),
            Collections.max(dates)
        };
    }

    /**
     * 특정 날짜 기준으로 bufferDays 이전까지의 대화 필터링
     * 
     * @param targetDate 기준 날짜 (이 날짜 포함)
     * @param bufferDays 기준 날짜로부터 이전 일수
     * @return 필터링된 날짜별 메시지 (targetDate - bufferDays ~ targetDate)
     * 
     * 예: targetDate = 2025-01-15, bufferDays = 7
     *     → 2025-01-08 ~ 2025-01-15 데이터 반환
     */
    public Map<LocalDate, List<KakaoTalkMessageDto>> filterByTargetDate(
            LocalDate targetDate,
            int bufferDays) {
        
        if (dailyChats.isEmpty()) {
            return new LinkedHashMap<>();
        }

        // targetDate가 null이면 가장 최신 날짜 사용
        LocalDate effectiveTargetDate = targetDate;
        if (effectiveTargetDate == null) {
            effectiveTargetDate = Collections.max(dailyChats.keySet());
        }

        LocalDate startDate = effectiveTargetDate.minusDays(bufferDays);
        LocalDate finalTargetDate = effectiveTargetDate;

        return dailyChats.entrySet().stream()
            .filter(e -> !e.getKey().isBefore(startDate) && !e.getKey().isAfter(finalTargetDate))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                LinkedHashMap::new
            ));
    }

    /**
     * 특정 기간의 대화만 필터링 (기존 방식 - 하위 호환용)
     * 
     * 두 가지 방식 지원:
     * 1. fromDate + days: 지정일로부터 N일 전까지 필터링
     * 2. startDate + endDate: 시작일 - bufferDays부터 종료일까지 필터링
     */
    public Map<LocalDate, List<KakaoTalkMessageDto>> filterByPeriod(
            int days, 
            LocalDate fromDate,
            LocalDate startDate,
            LocalDate endDate,
            int bufferDays) {
        
        if (dailyChats.isEmpty()) {
            return new LinkedHashMap<>();
        }

        // 새 방식: startDate와 endDate가 모두 제공된 경우
        if (startDate != null && endDate != null) {
            LocalDate actualStart = startDate.minusDays(bufferDays);
            return dailyChats.entrySet().stream()
                .filter(e -> !e.getKey().isBefore(actualStart) && !e.getKey().isAfter(endDate))
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (a, b) -> a,
                    LinkedHashMap::new
                ));
        }

        // 기존 방식: fromDate로부터 days일 전까지
        LocalDate effectiveFromDate = fromDate;
        if (effectiveFromDate == null) {
            effectiveFromDate = Collections.max(dailyChats.keySet());
        }

        LocalDate calcStartDate = effectiveFromDate.minusDays(days);
        LocalDate finalFromDate = effectiveFromDate;

        return dailyChats.entrySet().stream()
            .filter(e -> !e.getKey().isBefore(calcStartDate) && !e.getKey().isAfter(finalFromDate))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                LinkedHashMap::new
            ));
    }

    /**
     * 특정 발신자의 메시지만 필터링
     */
    public Map<LocalDate, List<KakaoTalkMessageDto>> filterBySender(
            String senderName, 
            Map<LocalDate, List<KakaoTalkMessageDto>> data) {
        
        Map<LocalDate, List<KakaoTalkMessageDto>> source = (data != null) ? data : dailyChats;
        Map<LocalDate, List<KakaoTalkMessageDto>> filtered = new LinkedHashMap<>();

        for (Map.Entry<LocalDate, List<KakaoTalkMessageDto>> entry : source.entrySet()) {
            List<KakaoTalkMessageDto> senderMessages = entry.getValue().stream()
                .filter(msg -> msg.getSender() != null && msg.getSender().contains(senderName))
                .collect(Collectors.toList());

            if (!senderMessages.isEmpty()) {
                filtered.put(entry.getKey(), senderMessages);
            }
        }

        return filtered;
    }

    /**
     * 구조화된 데이터를 다시 텍스트로 변환
     */
    public String toText(Map<LocalDate, List<KakaoTalkMessageDto>> data) {
        Map<LocalDate, List<KakaoTalkMessageDto>> source = (data != null) ? data : dailyChats;
        StringBuilder result = new StringBuilder();

        for (Map.Entry<LocalDate, List<KakaoTalkMessageDto>> entry : source.entrySet()) {
            result.append(String.format("--- %d년 %02d월 %02d일 ---\n",
                entry.getKey().getYear(),
                entry.getKey().getMonthValue(),
                entry.getKey().getDayOfMonth()));

            for (KakaoTalkMessageDto msg : entry.getValue()) {
                if (msg.getSender() != null) {
                    result.append(String.format("[%s] [%s] %s\n",
                        msg.getSender(), msg.getTime(), msg.getContent()));
                } else {
                    result.append(msg.getContent()).append("\n");
                }
            }
        }

        return result.toString();
    }

    /**
     * 대화 참여자 목록 반환
     */
    public List<String> getParticipants() {
        Set<String> senders = new TreeSet<>();
        for (List<KakaoTalkMessageDto> messages : dailyChats.values()) {
            for (KakaoTalkMessageDto msg : messages) {
                if (msg.getSender() != null) {
                    senders.add(msg.getSender());
                }
            }
        }
        return new ArrayList<>(senders);
    }

    /**
     * 대화 통계 반환
     */
    public ParsedChatDataDto getStatistics() {
        int totalMessages = dailyChats.values().stream()
            .mapToInt(List::size)
            .sum();

        List<String> participants = getParticipants();
        LocalDate[] dateRange = getDateRange();

        return ParsedChatDataDto.builder()
            .formatType(formatType)
            .totalDays(dailyChats.size())
            .totalMessages(totalMessages)
            .participants(participants)
            .participantCount(participants.size())
            .startDate(dateRange[0])
            .endDate(dateRange[1])
            .dailyChats(dailyChats)
            .build();
    }

    /**
     * 파일 형식 반환
     */
    public String getFormatType() {
        return formatType;
    }

    /**
     * 날짜별 채팅 데이터 반환
     */
    public Map<LocalDate, List<KakaoTalkMessageDto>> getDailyChats() {
        return dailyChats;
    }
}

