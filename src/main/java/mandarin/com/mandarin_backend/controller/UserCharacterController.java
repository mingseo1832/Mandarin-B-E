package mandarin.com.mandarin_backend.controller;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.UserCharacterRequestDto;
import mandarin.com.mandarin_backend.dto.UserCharacterResponseDto;
import mandarin.com.mandarin_backend.exception.CharacterNotFoundException;
import mandarin.com.mandarin_backend.exception.UserNotFoundException;
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

    // ----------------- 캐릭터 다건 조회 -----------------
    @GetMapping("/user/{id}")
    public ResponseEntity<?> getCharacters(@PathVariable Long id) {

        try {
            List<UserCharacterResponseDto> list =
                    characterService.getCharactersByUserId(id);

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("data", list);

            return ResponseEntity.ok(result);

        } catch (UserNotFoundException e) {
            return error(e.getMessage());
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

    // ----------------- 공통 에러 응답 -----------------
    private ResponseEntity<?> error(String msg) {
        return ResponseEntity.badRequest().body(
                Map.of("code", 400, "message", msg)
        );
    }
}
