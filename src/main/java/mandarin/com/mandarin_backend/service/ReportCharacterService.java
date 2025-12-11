package mandarin.com.mandarin_backend.service;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.ReportCharacterResponseDto;
import mandarin.com.mandarin_backend.entity.ReportCharacter;
import mandarin.com.mandarin_backend.repository.ReportCharacterRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportCharacterService {

    private final ReportCharacterRepository reportCharacterRepository;

    /**
     * 캐릭터 ID로 ReportCharacter 리스트 조회
     */
    public List<ReportCharacterResponseDto> getReportsByCharacterId(Long characterId) {
        List<ReportCharacter> list = reportCharacterRepository.findByCharacter_CharacterId(characterId);

        if (list.isEmpty()) {
            throw new IllegalArgumentException("해당 캐릭터의 리포트가 없습니다.");
        }

        return list.stream()
                .map(ReportCharacterResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
}

