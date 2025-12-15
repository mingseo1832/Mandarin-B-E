package mandarin.com.mandarin_backend.controller;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.ReportCharacterResponseDto;
import mandarin.com.mandarin_backend.dto.UserCharacterRequestDto;
import mandarin.com.mandarin_backend.dto.UserCharacterResponseDto;
import mandarin.com.mandarin_backend.exception.CharacterNotFoundException;
import mandarin.com.mandarin_backend.exception.UserNotFoundException;
import mandarin.com.mandarin_backend.service.ReportCharacterService;
import mandarin.com.mandarin_backend.service.UserCharacterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/character")
public class UserCharacterController {

    private final UserCharacterService characterService;
    private final ReportCharacterService reportCharacterService;

    // ----------------- 캐릭터 다건 조회 -----------------
    @GetMapping("/user/{id}")
public ResponseEntity<?> getCharacters(@PathVariable Long id) {
    try {
        // 1. 서비스 호출
        List<UserCharacterResponseDto> list = characterService.getCharactersByUserId(id);

        // 2. 결과 맵 생성
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", list);

        // 3. 정상 반환
        return ResponseEntity.ok(result);

    } catch (UserNotFoundException e) {
        // 유저가 없을 때
        return ResponseEntity.status(404).body(e.getMessage());
        
    } catch (Exception e) { 
        // 🚨 중요: 여기서 나머지 모든 에러(Null ID 등)를 잡아서 메시지를 확인해야 합니다.
        e.printStackTrace(); // 콘솔에 에러 원인 출력
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

    // ----------------- 캐릭터 생성 -----------------
    @PostMapping("/create")
    public ResponseEntity<?> createCharacter(
            @RequestPart("json") UserCharacterRequestDto dto,
            @RequestPart(value = "character_img", required = false) MultipartFile characterImg,
            @RequestPart(value = "full_dialogue", required = false) MultipartFile fullDialogue
    ) {

        try {
            characterService.createCharacter(dto, characterImg, fullDialogue);

            return ResponseEntity.ok(Map.of("code", 200));

        } catch (UserNotFoundException | IOException e) {
            return error(e.getMessage());
        }
    }

    // ----------------- 캐릭터 수정 -----------------
    @PostMapping("/update/{characterId}")
    public ResponseEntity<?> updateCharacter(
            @PathVariable Long characterId,
            @RequestPart("json") UserCharacterRequestDto dto,
            @RequestPart(value = "character_img", required = false) MultipartFile characterImg,
            @RequestPart(value = "full_dialogue", required = false) MultipartFile fullDialogue
    ) {

        try {
            characterService.updateCharacter(characterId, dto, characterImg, fullDialogue);
            return ResponseEntity.ok(Map.of("code", 200));

        } catch (CharacterNotFoundException | IOException e) {
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
            List<ReportCharacterResponseDto> list =
                    reportCharacterService.getReportsByCharacterId(characterId);

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
