package mandarin.com.mandarin_backend.service;

import mandarin.com.mandarin_backend.dto.HistorySumResponseDto;
import mandarin.com.mandarin_backend.dto.UserCharacterResponseDto;
import mandarin.com.mandarin_backend.entity.User;
import mandarin.com.mandarin_backend.entity.UserCharacter;
import mandarin.com.mandarin_backend.exception.UserNotFoundException;
import mandarin.com.mandarin_backend.repository.UserCharacterRepository;
import mandarin.com.mandarin_backend.repository.UserRepository;
import mandarin.com.mandarin_backend.util.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserCharacterService 테스트")
class UserCharacterServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCharacterRepository userCharacterRepository;

    @Mock
    private WebClient webClient;
    
    @Mock
    private FileUtil fileUtil;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private UserCharacterService userCharacterService;

    private User testUser;
    private UserCharacter testCharacter1;
    private UserCharacter testCharacter2;

    @BeforeEach
    void setUp() {
        // 테스트용 User 생성
        testUser = User.builder()
                .id(1L)
                .userId("testuser")
                .username("테스트")
                .password("password123")
                .loveType(5)
                .build();

        // 테스트용 UserCharacter 생성
        testCharacter1 = UserCharacter.builder()
                .characterId(1L)
                .user(testUser)
                .characterName("테스트캐릭터1")
                .characterAge(25)
                .relationType(1)
                .kakaoName("카카오이름1")
                .fullDialogue("대화내용1")
                .historySum("요약1")
                .build();

        testCharacter2 = UserCharacter.builder()
                .characterId(2L)
                .user(testUser)
                .characterName("테스트캐릭터2")
                .characterAge(30)
                .relationType(0)
                .kakaoName("카카오이름2")
                .fullDialogue("대화내용2")
                .historySum("요약2")
                .build();
    }

    @Test
    @DisplayName("사용자 ID로 캐릭터 목록 조회 - 성공")
    void getCharactersByUserId_Success() {
        // given
        Long userId = 1L;
        List<UserCharacter> characters = Arrays.asList(testCharacter1, testCharacter2);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userCharacterRepository.findByUser(testUser)).thenReturn(characters);

        // when
        List<UserCharacterResponseDto> result = userCharacterService.getCharactersByUserId(userId);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("테스트캐릭터1", result.get(0).getCharacterName());
        assertEquals("카카오이름1", result.get(0).getKakaoName());
        assertEquals("테스트캐릭터2", result.get(1).getCharacterName());
        assertEquals("카카오이름2", result.get(1).getKakaoName());

        verify(userRepository, times(1)).findById(userId);
        verify(userCharacterRepository, times(1)).findByUser(testUser);
    }

    @Test
    @DisplayName("사용자 ID로 캐릭터 목록 조회 - 사용자가 존재하지 않음")
    void getCharactersByUserId_UserNotFound() {
        // given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userCharacterService.getCharactersByUserId(userId)
        );

        assertEquals("회원 정보가 없습니다.", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(userCharacterRepository, never()).findByUser(any());
    }

    @Test
    @DisplayName("사용자 ID로 캐릭터 목록 조회 - 빈 리스트 반환")
    void getCharactersByUserId_EmptyList() {
        // given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userCharacterRepository.findByUser(testUser)).thenReturn(Arrays.asList());

        // when
        List<UserCharacterResponseDto> result = userCharacterService.getCharactersByUserId(userId);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findById(userId);
        verify(userCharacterRepository, times(1)).findByUser(testUser);
    }

    @Test
    @DisplayName("히스토리 요약 및 저장 - 성공")
    void summarizeAndSaveHistory_Success() {
        // given
        Long characterId = 1L;
        String history = "대화 히스토리 내용";
        String summary = "요약된 히스토리";

        HistorySumResponseDto responseDto = HistorySumResponseDto.builder()
                .summary(summary)
                .build();

        when(userCharacterRepository.findById(characterId))
                .thenReturn(Optional.of(testCharacter1));
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        doReturn(requestBodySpec).when(requestBodySpec).bodyValue(any());
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(HistorySumResponseDto.class)))
                .thenReturn(Mono.just(responseDto));
        when(userCharacterRepository.save(any(UserCharacter.class)))
                .thenReturn(testCharacter1);

        // when
        UserCharacter result = userCharacterService.summarizeAndSaveHistory(characterId, history);

        // then
        assertNotNull(result);
        assertEquals(summary, result.getHistorySum());
        verify(userCharacterRepository, times(1)).findById(characterId);
        verify(webClient, times(1)).post();
        verify(userCharacterRepository, times(1)).save(any(UserCharacter.class));
    }

    @Test
    @DisplayName("히스토리 요약 및 저장 - 캐릭터가 존재하지 않음")
    void summarizeAndSaveHistory_CharacterNotFound() {
        // given
        Long characterId = 999L;
        String history = "대화 히스토리 내용";

        when(userCharacterRepository.findById(characterId))
                .thenReturn(Optional.empty());

        // when & then
        mandarin.com.mandarin_backend.exception.CharacterNotFoundException exception = assertThrows(
                mandarin.com.mandarin_backend.exception.CharacterNotFoundException.class,
                () -> userCharacterService.summarizeAndSaveHistory(characterId, history)
        );

        assertTrue(exception.getMessage().contains("캐릭터를 찾을 수 없습니다"));
        verify(userCharacterRepository, times(1)).findById(characterId);
        verify(webClient, never()).post();
        verify(userCharacterRepository, never()).save(any());
    }

    @Test
    @DisplayName("히스토리 요약 및 저장 - Python 서버 응답이 null")
    void summarizeAndSaveHistory_NullResponse() {
        // given
        Long characterId = 1L;
        String history = "대화 히스토리 내용";

        when(userCharacterRepository.findById(characterId))
                .thenReturn(Optional.of(testCharacter1));
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        doReturn(requestBodySpec).when(requestBodySpec).bodyValue(any());
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(HistorySumResponseDto.class)))
                .thenReturn(Mono.empty());

        // when & then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userCharacterService.summarizeAndSaveHistory(characterId, history)
        );

        assertEquals("히스토리 요약 실패.", exception.getMessage());
        verify(userCharacterRepository, times(1)).findById(characterId);
        verify(webClient, times(1)).post();
        verify(userCharacterRepository, never()).save(any());
    }

    @Test
    @DisplayName("히스토리 요약 및 저장 - Python 서버 응답의 summary가 null")
    void summarizeAndSaveHistory_NullSummary() {
        // given
        Long characterId = 1L;
        String history = "대화 히스토리 내용";

        HistorySumResponseDto responseDto = HistorySumResponseDto.builder()
                .summary(null)
                .build();

        when(userCharacterRepository.findById(characterId))
                .thenReturn(Optional.of(testCharacter1));
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        doReturn(requestBodySpec).when(requestBodySpec).bodyValue(any());
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(HistorySumResponseDto.class)))
                .thenReturn(Mono.just(responseDto));

        // when & then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userCharacterService.summarizeAndSaveHistory(characterId, history)
        );

        assertEquals("히스토리 요약 실패.", exception.getMessage());
        verify(userCharacterRepository, times(1)).findById(characterId);
        verify(webClient, times(1)).post();
        verify(userCharacterRepository, never()).save(any());
    }
}

