package mandarin.com.mandarin_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper; // [추가] JSON 변환 라이브러리
import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.ParsedChatDataDto;
import mandarin.com.mandarin_backend.dto.ReportCharacterResponseDto;
import mandarin.com.mandarin_backend.dto.UserCharacterRequestDto;
import mandarin.com.mandarin_backend.dto.UserCharacterResponseDto;
import mandarin.com.mandarin_backend.entity.UserCharacter;
import mandarin.com.mandarin_backend.exception.CharacterNotFoundException;
import mandarin.com.mandarin_backend.exception.UserNotFoundException;
import mandarin.com.mandarin_backend.service.AnalysisService;
import mandarin.com.mandarin_backend.service.KakaoTalkParseService;
import mandarin.com.mandarin_backend.service.ReportCharacterService;
import mandarin.com.mandarin_backend.service.UserCharacterService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/character")
public class UserCharacterController {

    private final UserCharacterService characterService;
    private final ReportCharacterService reportCharacterService;
    private final AnalysisService analysisService;
    private final KakaoTalkParseService kakaoTalkParseService;

    // ----------------- 캐릭터 다건 조회 -----------------
    // [수정] GET 요청에는 consumes = MediaType.MULTIPART_FORM_DATA_VALUE 가 필요 없습니다. 제거했습니다.
    @GetMapping("/user/{id}") 
    public ResponseEntity<?> getCharacters(@PathVariable Long id) {
        try {
            List<UserCharacterResponseDto> list = characterService.getCharactersByUserId(id);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", list);

            return ResponseEntity.ok(result);

        } catch (UserNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("서버 에러 발생: " + e.getMessage());
        }
    }

    // ----------------- 캐릭터 단건 조회 -----------------
    @GetMapping("/{characterId}")
    public ResponseEntity<?> getCharacter(@PathVariable Long characterId) {
        try {
            UserCharacterResponseDto dto = characterService.getCharacter(characterId);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", dto);
            return ResponseEntity.ok(result);

        } catch (CharacterNotFoundException e) {
            return error(e.getMessage());
        }
    }

    // ----------------- 캐릭터 생성 (핵심 수정 부분) -----------------
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createCharacter(
            @RequestPart("json") String jsonStr, // [수정 1] DTO 대신 String으로 받음 (415 해결)
            @RequestPart(value = "character_img", required = false) MultipartFile characterImg,
            @RequestPart(value = "full_dialogue", required = false) MultipartFile fullDialogue
    ) {
        // [수정 2] ObjectMapper 생성
        ObjectMapper objectMapper = new ObjectMapper();
        UserCharacterRequestDto dto = null;

        try {
            // [수정 3] 문자열(jsonStr)을 자바 객체(DTO)로 변환
            dto = objectMapper.readValue(jsonStr, UserCharacterRequestDto.class);

            // 서비스 호출
            UserCharacter savedCharacter = characterService.createCharacter(dto, characterImg, fullDialogue);

            // 캐릭터 리포트 생성 (fullDialogue 파일이 있는 경우)
            if (fullDialogue != null && !fullDialogue.isEmpty() && dto.getKakaoName() != null) {
                try {
                    // 1. 대화 파일 파싱하여 참여자 목록 조회
                    String rawTextContent = new String(fullDialogue.getBytes(), StandardCharsets.UTF_8);
                    ParsedChatDataDto parsedData = kakaoTalkParseService.parseInfo(rawTextContent);
                    System.out.println("--------------------------------");
                    System.out.println("--------------------------------");
                    System.out.println("--------------------------------");
                    System.out.println("parsedData: " + fullDialogue);
                    
                    // 2. 참여자 목록에서 상대방 찾기 (kakaoName 제외)
                    List<String> participants = parsedData.getParticipants();
                    String kakaoName = dto.getKakaoName();
                    String targetName = participants.stream()
                        .filter(name -> !name.equals(kakaoName))
                        .findFirst()
                        .orElse(null);
                    
                    if (targetName != null) {
                        // 3. 리포트 생성
                        analysisService.createReportCharacterFromFullDialogue(
                            savedCharacter,
                            kakaoName,
                            targetName
                        );
                        System.out.println("[Create] 캐릭터 리포트 생성 완료 - 캐릭터ID: " + savedCharacter.getCharacterId());
                    } else {
                        System.out.println("[Create] 상대방을 찾을 수 없어 리포트를 생성하지 않습니다. 참여자: " + participants);
                    }
                } catch (Exception e) {
                    System.err.println("[Create] 캐릭터 리포트 생성 실패: " + e.getMessage());
                    // 리포트 생성 실패해도 캐릭터 생성은 성공으로 처리
                }
            }

            return ResponseEntity.ok(Map.of("code", 200));

        } catch (IOException e) {
            // JSON 파싱 실패 혹은 파일 에러
            return error("데이터 형식 오류: " + e.getMessage());
        } catch (UserNotFoundException e) {
            return error(e.getMessage());
        }
    }

    // ----------------- 캐릭터 수정 (여기도 똑같이 수정) -----------------
    @PostMapping(value = "/update/{characterId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateCharacter(
            @PathVariable Long characterId,
            @RequestPart("json") String jsonStr, // [수정 1] String으로 변경
            @RequestPart(value = "character_img", required = false) MultipartFile characterImg,
            @RequestPart(value = "full_dialogue", required = false) MultipartFile fullDialogue
    ) {
        ObjectMapper objectMapper = new ObjectMapper();
        UserCharacterRequestDto dto = null;

        try {
            // [수정 2] 수동 변환
            dto = objectMapper.readValue(jsonStr, UserCharacterRequestDto.class);

            characterService.updateCharacter(characterId, dto, characterImg, fullDialogue);
            return ResponseEntity.ok(Map.of("code", 200));

        } catch (IOException e) {
            return error("데이터 형식 오류: " + e.getMessage());
        } catch (CharacterNotFoundException e) {
            return error(e.getMessage());
        }
    }

    // ----------------- 캐릭터 삭제 -----------------
    @DeleteMapping("/delete/{characterId}")
    public ResponseEntity<?> deleteCharacter(@PathVariable Long characterId) {
        try {
            characterService.deleteCharacter(characterId);
            return ResponseEntity.ok(Map.of("code", 200));

        } catch (CharacterNotFoundException e) {
            return error(e.getMessage());
        }
    }

    // ----------------- 캐릭터 리포트 조회 -----------------
    @GetMapping("/report/{character_id}")
    public ResponseEntity<?> getCharacterReports(@PathVariable("character_id") Long characterId) {
        try {
            List<ReportCharacterResponseDto> list = reportCharacterService.getReportsByCharacterId(characterId);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", list);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    // ----------------- 공통 에러 응답 -----------------
    private ResponseEntity<?> error(String msg) {
        return ResponseEntity.badRequest().body(
                Map.of("code", 400, "message", msg)
        );
    }
}
