package com.example.petlog.service.impl;

import com.example.petlog.client.NotificationServiceClient;
import com.example.petlog.dto.request.RecapRequest;
import com.example.petlog.dto.response.RecapResponse;
import com.example.petlog.entity.Recap;
import com.example.petlog.entity.RecapHighlight;
import com.example.petlog.entity.RecapStatus;
import com.example.petlog.exception.EntityNotFoundException;
import com.example.petlog.exception.ErrorCode;
import com.example.petlog.repository.RecapRepository;
import com.example.petlog.service.RecapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecapServiceImpl implements RecapService {

    private final RecapRepository recapRepository;
    private final NotificationServiceClient notificationClient;

    @Override
    @Transactional
    public Long createRecap(RecapRequest.Create request) {
        // 1. 엔티티 빌드
        Recap recap = Recap.builder()
                .userId(request.getUserId())
                .petId(request.getPetId())
                .title(request.getTitle())
                .summary(request.getSummary())
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .mainImageUrl(request.getMainImageUrl())
                .momentCount(request.getMomentCount())
                .status(RecapStatus.GENERATED)
                .build();

        // 2. 하이라이트 추가
        if (request.getHighlights() != null) {
            for (RecapRequest.HighlightDto h : request.getHighlights()) {
                recap.addHighlight(RecapHighlight.builder()
                        .title(h.getTitle())
                        .content(h.getContent())
                        .build());
            }
        }

        // 3. 저장
        Recap savedRecap = recapRepository.save(recap);

        // 4. 알림 발송 (로컬 테스트를 위해 임시 주석 처리)
        // 알림 서비스가 없어도 에러가 나지 않도록 주석 처리했습니다.
        /*
        try {
            notificationClient.sendNotification(new NotificationServiceClient.NotificationRequest(
                    request.getUserId(),
                    "✨ 월간 리캡이 도착했습니다!",
                    request.getTitle() + "의 추억을 확인해보세요.",
                    "RECAP_CREATED"
            ));
        } catch (Exception e) {
            log.warn("알림 서비스 호출 실패: {}", e.getMessage());
        }
        */

        return savedRecap.getRecapId();
    }

    @Override
    public RecapResponse.Detail getRecapDetail(Long recapId) {
        Recap recap = recapRepository.findById(recapId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.RECAP_NOT_FOUND));

        // 1. DTO로 1차 변환 (이때 건강 데이터는 비어있음)
        RecapResponse.Detail response = RecapResponse.Detail.from(recap);

        // 2. [TODO] 헬스케어 서비스 호출하여 건강 데이터 채우기 (추후 구현)
        /*
        try {
            // HealthReportDto healthData = healthClient.getReport(recap.getPetId(), recap.getPeriodStart(), recap.getPeriodEnd());
            // if (healthData != null) {
            //      response.setAvgHeartRate(healthData.getHeartRate());
            //      response.setAvgStepCount(healthData.getStepCount());
            //      response.setAvgSleepTime(healthData.getSleepTime());
            //      response.setAvgWeight(healthData.getWeight());
            // }
        } catch (Exception e) {
            log.warn("헬스케어 데이터 조회 실패 (리캡 상세 조회는 계속 진행): {}", e.getMessage());
        }
        */

        return response;
    }

    @Override
    public List<RecapResponse.Simple> getRecaps(Long userId) {
        return recapRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(RecapResponse.Simple::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<RecapResponse.Simple> getRecapsByPet(Long petId) {
        return recapRepository.findAllByPetIdOrderByCreatedAtDesc(petId).stream()
                .map(RecapResponse.Simple::from)
                .collect(Collectors.toList());
    }
}