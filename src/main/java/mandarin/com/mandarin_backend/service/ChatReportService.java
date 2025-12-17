package mandarin.com.mandarin_backend.service;

import lombok.RequiredArgsConstructor;
import mandarin.com.mandarin_backend.dto.ChatReportCreateRequestDto;
import mandarin.com.mandarin_backend.dto.ChatReportResponseDto;
import mandarin.com.mandarin_backend.entity.*;
import mandarin.com.mandarin_backend.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatReportService {

    private final ChatReportRepository chatReportRepository;
    private final SimulationRepository simulationRepository;
    private final UserRepository userRepository;
    private final ChatReportAvgRepository chatReportAvgRepository;

    // ----------------------------
    // 1. ChatReport 단건 조회
    // ----------------------------
    public ChatReportResponseDto getChatReportById(Integer id) {
        ChatReport report = chatReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("리포트 정보가 없습니다."));
        return ChatReportResponseDto.fromEntity(report);
    }

    // ----------------------------
    // 2. 유저별 ChatReport 리스트 조회
    // ----------------------------
    public List<ChatReportResponseDto> getChatReportsByUserId(Long userId) {
        List<ChatReport> list = chatReportRepository.findByUser_Id(userId);

        if (list.isEmpty()) {
            throw new IllegalArgumentException("해당 유저의 리포트가 없습니다.");
        }

        return list.stream()
                .map(ChatReportResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    // ----------------------------
    // 3. 캐릭터별 ChatReport 리스트 조회
    // ----------------------------
    public List<ChatReportResponseDto> getChatReportsByCharacterId(Long characterId) {
        List<ChatReport> list = chatReportRepository.findByCharacter_CharacterId(characterId);

        if (list.isEmpty()) {
            throw new IllegalArgumentException("해당 캐릭터의 리포트가 없습니다.");
        }

        return list.stream()
                .map(ChatReportResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    // ----------------------------
    // 4. Chat Report 생성 (POST)
    // ----------------------------
    public ChatReportResponseDto createChatReport(ChatReportCreateRequestDto request) {

        // 1. Simulation 조회
        Simulation simulation = simulationRepository.findById(request.getSimulation_id())
                .orElseThrow(() -> new IllegalArgumentException("Simulation not found"));

        // 2. User 조회
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserCharacter character = simulation.getCharacter();

        // 3. ChatReport 생성
        ChatReport report = ChatReport.builder()
                .simulation(simulation)
                .user(user)
                .character(character)
                .scoreAvg(80)             // TODO: 점수 계산 후 적용
                .labelKey(1)              // TODO
                .labelScore(50)           // TODO
                .reportContent("{}")
                .build();

        chatReportRepository.save(report);

        // 4. Simulation 종료 표시
        simulation.setIsFinished(true);
        simulationRepository.save(simulation);

        // 5. ChatReportAvg 처리
        List<ChatReportAvg> avgList = chatReportAvgRepository.findByUser_Id(user.getId());

        // labelKey를 안전하게 F1~F6으로 변환
        Integer labelKey = report.getLabelKey();
        if (labelKey == null || labelKey < 1 || labelKey > 6) {
            labelKey = 1; // 기본값
        }
        ChatReportAvg.TotalLabelKey totalLabelKey = ChatReportAvg.TotalLabelKey.valueOf("F" + labelKey);

        if (avgList.isEmpty()) {
            // 새로 생성
            ChatReportAvg newAvg = ChatReportAvg.builder()
                    .chatReport(report)
                    .user(user)
                    .avgMandarinScore(report.getScoreAvg())
                    .totalLabelKey(totalLabelKey)
                    .totalLabelScore(report.getLabelScore())
                    .build();

            chatReportAvgRepository.save(newAvg);
        } else {
            // 기존 평균 업데이트 (첫 번째 항목 사용)
            ChatReportAvg avgRow = avgList.get(0);

            int newAvgScore = (avgRow.getAvgMandarinScore() + report.getScoreAvg()) / 2;

            avgRow.setAvgMandarinScore(newAvgScore);
            avgRow.setChatReport(report);
            avgRow.setTotalLabelKey(totalLabelKey);
            avgRow.setTotalLabelScore(report.getLabelScore());

            chatReportAvgRepository.save(avgRow);
        }

        return ChatReportResponseDto.fromEntity(report);
    }
}
