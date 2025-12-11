package mandarin.com.mandarin_backend.service;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.ChatReportResponseDto;
import mandarin.com.mandarin_backend.entity.ChatReport;
import mandarin.com.mandarin_backend.repository.ChatReportRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatReportService {

    private final ChatReportRepository chatReportRepository;

    // 리포트 단건 조회
    public ChatReportResponseDto getChatReportById(Integer id) {
        ChatReport report = chatReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("리포트 정보가 없습니다."));

        return ChatReportResponseDto.fromEntity(report);
    }

    // 유저별 리포트 조회
    public List<ChatReportResponseDto> getChatReportsByUserId(Long userId) {
        List<ChatReport> list = chatReportRepository.findByUser_Id(userId);

        if (list.isEmpty()) {
            throw new IllegalArgumentException("해당 유저의 리포트가 없습니다.");
        }

        return list.stream()
                .map(ChatReportResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 캐릭터별 리포트 조회
    public List<ChatReportResponseDto> getChatReportsByCharacterId(Long characterId) {
        List<ChatReport> list = chatReportRepository.findByCharacter_CharacterId(characterId);

        if (list.isEmpty()) {
            throw new IllegalArgumentException("해당 캐릭터의 리포트가 없습니다.");
        }

        return list.stream()
                .map(ChatReportResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
}
