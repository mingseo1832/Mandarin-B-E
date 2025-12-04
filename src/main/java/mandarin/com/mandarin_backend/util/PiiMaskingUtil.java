package mandarin.com.mandarin_backend.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PiiMaskingUtil {

    // =================================================================================
    // 1. 정규표현식 정의
    // =================================================================================

    // 전화번호 (010-1234-5678, 02-123-4567, 01012345678 등)
    // 수정: 전체 전화번호를 매칭하도록 변경
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "(01[016789]|02|0[3-9][0-9])[- .]?(\\d{3,4})[- .]?(\\d{4})"
    );

    // 주민등록번호 (900101-1234567, 뒷자리 1~4로 시작)
    private static final Pattern RRN_PATTERN = Pattern.compile("\\d{6}[- .]?[1-4]\\d{6}");

    // 이메일 (test@example.com)
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}");

    // 계좌번호 (연속된 숫자와 하이픈의 조합, 11자리 이상)
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("\\d{3,6}[-]\\d{2,6}[-]\\d{3,6}([-]?\\d{1,5})?");

    // 카드번호 (1234-5678-9012-3456 또는 1234567890123456)
    private static final Pattern CARD_PATTERN = Pattern.compile(
        "\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}"
    );

    // 비밀번호 (키워드 감지 방식)
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "(?i)(비번|비밀번호|패스워드|pw|password|pass|pin|code)\\s*[:=]?\\s*([a-zA-Z0-9!@#$%^&*()_+\\-=]+)"
    );

    // URL (위치정보, 개인정보 포함 가능)
    private static final Pattern URL_PATTERN = Pattern.compile(
        "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)"
    );

    // =================================================================================
    // 한국 주소 패턴
    // =================================================================================
    
    // 시/도 목록
    private static final String SIDO = 
        "서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|전남|경북|경남|제주";
    
    // 도로명주소: (시도)? (시군구)? (도로명) (번호) (상세주소)?
    // 예: 서울 강남구 테헤란로 123, 경기도 성남시 분당구 판교로 123길 45
    private static final Pattern ROAD_ADDRESS_PATTERN = Pattern.compile(
        "(?:" + SIDO + ")(?:특별시|광역시|특별자치시|특별자치도|도)?\\s*" +  // 시도
        "(?:[가-힣]+(?:시|군|구)\\s*)*" +                                    // 시군구
        "[가-힣0-9]+(?:로|길)\\s*\\d+(?:-\\d+)?" +                          // 도로명+번호
        "(?:\\s*,?\\s*[가-힣0-9]+(?:동|층|호|빌딩|타워|아파트|오피스텔))?",  // 상세주소(선택)
        Pattern.UNICODE_CHARACTER_CLASS
    );
    
    // 지번주소: (시도)? (시군구)? (동/읍/면/리) (번지)
    // 예: 서울 강남구 역삼동 123-45, 역삼동 123번지
    private static final Pattern JIBUN_ADDRESS_PATTERN = Pattern.compile(
        "(?:(?:" + SIDO + ")(?:특별시|광역시|특별자치시|특별자치도|도)?\\s*)?" +  // 시도(선택)
        "(?:[가-힣]+(?:시|군|구)\\s*)*" +                                        // 시군구(선택)
        "[가-힣0-9]+(?:동|읍|면|리)\\s*" +                                       // 동/읍/면/리
        "\\d+(?:-\\d+)?(?:번지)?",                                               // 번지
        Pattern.UNICODE_CHARACTER_CLASS
    );
    
    // 건물 상세주소: OO빌딩 3층 301호, OO아파트 102동 1503호
    private static final Pattern BUILDING_DETAIL_PATTERN = Pattern.compile(
        "[가-힣a-zA-Z0-9]+(?:빌딩|타워|센터|아파트|오피스텔|주상복합|상가)\\s*" +
        "(?:\\d+동\\s*)?\\d+(?:층|호)",
        Pattern.UNICODE_CHARACTER_CLASS
    );

    /**
     * 개인정보가 포함된 문자열을 받아 마스킹 처리하여 반환합니다.
     * 처리 순서: 비밀번호 -> 주민번호 -> 카드번호 -> 전화번호 -> 이메일 -> 계좌번호 -> URL
     */
    public static String mask(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String masked = content;

        // -----------------------------------------------------------
        // 1. 비밀번호 마스킹 (키워드 기반이라 가장 먼저 처리 권장)
        // 예: "비번 1234" -> "비번 ****"
        // -----------------------------------------------------------
        Matcher pwMatcher = PASSWORD_PATTERN.matcher(masked);
        while (pwMatcher.find()) {
            String keyword = pwMatcher.group(1);
            String replacement = keyword + " ****";
            masked = masked.replace(pwMatcher.group(), replacement);
        }

        // -----------------------------------------------------------
        // 2. 주민등록번호 마스킹
        // 예: "900101-1234567" -> "******-*******"
        // -----------------------------------------------------------
        masked = RRN_PATTERN.matcher(masked).replaceAll("******-*******");

        // -----------------------------------------------------------
        // 3. 카드번호 마스킹 (계좌번호보다 먼저 처리)
        // 예: "1234-5678-9012-3456" -> "****-****-****-****"
        // -----------------------------------------------------------
        Matcher cardMatcher = CARD_PATTERN.matcher(masked);
        while (cardMatcher.find()) {
            String card = cardMatcher.group();
            String replacement = card.replaceAll("\\d", "*");
            masked = masked.replace(card, replacement);
        }

        // -----------------------------------------------------------
        // 4. 전화번호 마스킹
        // 예: "010-1234-5678" -> "010-****-****"
        // -----------------------------------------------------------
        Matcher phoneMatcher = PHONE_PATTERN.matcher(masked);
        StringBuffer phoneBuffer = new StringBuffer();
        while (phoneMatcher.find()) {
            String areaCode = phoneMatcher.group(1);  // 010, 02 등
            // 중간 번호와 끝 번호는 마스킹
            String replacement = areaCode + "-****-****";
            phoneMatcher.appendReplacement(phoneBuffer, replacement);
        }
        phoneMatcher.appendTail(phoneBuffer);
        masked = phoneBuffer.toString();

        // -----------------------------------------------------------
        // 5. 이메일 마스킹
        // 예: "test@naver.com" -> "te**@****.***"
        // -----------------------------------------------------------
        Matcher emailMatcher = EMAIL_PATTERN.matcher(masked);
        while (emailMatcher.find()) {
            String email = emailMatcher.group();
            int atIndex = email.indexOf("@");
            
            String replacement;
            if (atIndex > 2) {
                String prefix = email.substring(0, 2);
                replacement = prefix + "**@****.***";
            } else {
                replacement = "***@****.***";
            }
            masked = masked.replace(email, replacement);
        }

        // -----------------------------------------------------------
        // 6. 계좌번호 마스킹
        // 예: "110-123-456789" -> "***-***-******"
        // 주의: 하이픈이 포함된 11자리 이상만 처리 (오탐 방지)
        // -----------------------------------------------------------
        Matcher accountMatcher = ACCOUNT_PATTERN.matcher(masked);
        while (accountMatcher.find()) {
            String account = accountMatcher.group();
            
            // 오탐 방지: 이미 마스킹된 부분(*포함)은 건너뛰기
            if (!account.contains("*") && account.length() >= 11) {
                String replacement = account.replaceAll("\\d", "*");
                masked = masked.replace(account, replacement);
            }
        }

        // -----------------------------------------------------------
        // 7. URL 마스킹 (위치정보, 개인정보 포함 가능)
        // 예: "https://map.naver.com/..." -> "[URL 마스킹됨]"
        // -----------------------------------------------------------
        masked = URL_PATTERN.matcher(masked).replaceAll("[URL 마스킹됨]");

        // -----------------------------------------------------------
        // 8. 도로명주소 마스킹
        // 예: "서울 강남구 테헤란로 123" -> "[주소 마스킹됨]"
        // -----------------------------------------------------------
        masked = ROAD_ADDRESS_PATTERN.matcher(masked).replaceAll("[주소 마스킹됨]");

        // -----------------------------------------------------------
        // 9. 지번주소 마스킹
        // 예: "역삼동 123-45" -> "[주소 마스킹됨]"
        // -----------------------------------------------------------
        masked = JIBUN_ADDRESS_PATTERN.matcher(masked).replaceAll("[주소 마스킹됨]");

        // -----------------------------------------------------------
        // 10. 건물 상세주소 마스킹
        // 예: "OO빌딩 3층 301호" -> "[상세주소 마스킹됨]"
        // -----------------------------------------------------------
        masked = BUILDING_DETAIL_PATTERN.matcher(masked).replaceAll("[상세주소 마스킹됨]");

        return masked;
    }
}