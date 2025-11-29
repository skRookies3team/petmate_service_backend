package com.example.petlog.service.impl;

import com.example.petlog.client.PetServiceClient;
import com.example.petlog.client.UserServiceClient;
import com.example.petlog.dto.request.DiaryRequest;
import com.example.petlog.dto.response.DiaryResponse;
import com.example.petlog.entity.Diary;
import com.example.petlog.entity.DiaryImage;
import com.example.petlog.exception.EntityNotFoundException;
import com.example.petlog.exception.ErrorCode;
import com.example.petlog.repository.DiaryRepository;
import com.example.petlog.service.DiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserServiceClient userClient;
    private final PetServiceClient petClient;

    @Override
    @Transactional
    public Long createDiary(DiaryRequest.Create request) {
//        // 1. MSA 검증: User와 Pet이 실제 존재하는지 확인
//        if (!userClient.checkUserExists(request.getUserId())) {
//            throw new EntityNotFoundException(ErrorCode.USER_NOT_FOUND);
//        }
//        if (!petClient.checkPetExists(request.getPetId())) {
//            throw new EntityNotFoundException(ErrorCode.PET_NOT_FOUND);
//        }

        // 2. 일기 엔티티 생성
        Diary diary = Diary.builder()
                .userId(request.getUserId())
                .petId(request.getPetId())
                .content(request.getContent())
                .visibility(request.getVisibility())
                .isAiGen(request.getIsAiGen())
                .weather(request.getWeather())
                .mood(request.getMood())
                .build();

        // 3. 이미지 리스트 처리
        if (request.getImages() != null) {
            // DiaryImageRequest -> DiaryRequest.Image
            for (DiaryRequest.Image imgRequest : request.getImages()) {
                diary.addImage(DiaryImage.builder()
                        .imageUrl(imgRequest.getImageUrl())
                        .imgOrder(imgRequest.getImgOrder())
                        .mainImage(imgRequest.getMainImage())
                        .build());
            }
        }

        return diaryRepository.save(diary).getDiaryId();
    }

    @Override
    public DiaryResponse getDiary(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));

        // Entity -> DTO 변환 (DiaryResponse 내부의 from 메서드 활용)
        return DiaryResponse.from(diary);
    }

    @Override
    @Transactional
    public void updateDiary(Long diaryId, DiaryRequest.Update request) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));

        // Dirty Checking을 통한 업데이트
        diary.update(
                request.getContent(),
                request.getVisibility(),
                request.getWeather(),
                request.getMood()
        );

        // TODO: 이미지 수정 로직이 필요하다면 별도 메서드나 여기서 처리 추가
    }

    @Override
    @Transactional
    public void deleteDiary(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.DIARY_NOT_FOUND));

        // Cascade.ALL 설정에 의해 연관된 DiaryImage들도 함께 삭제됨
        diaryRepository.delete(diary);
    }
}