package com.twodo0.capstoneWeb.image.controller;

import com.twodo0.capstoneWeb.image.domain.ImageMeta;
import com.twodo0.capstoneWeb.image.dto.UploadResponseDto;
import com.twodo0.capstoneWeb.common.port.PresignUrlPort;
import com.twodo0.capstoneWeb.image.service.ImageService;
import com.twodo0.capstoneWeb.rental.domain.enums.ImageSlot;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;
    private final PresignUrlPort presignUrlPort;

    @PostMapping
    public UploadResponseDto upload(@RequestParam("file") MultipartFile file){ // 멀티파트 필드 이름과 매핑되어야함
        ImageMeta uploadedImage = imageService.upload(file);
        var url = presignUrlPort.presignGet(uploadedImage.getBucket(), uploadedImage.getKey());
        return UploadResponseDto
                .builder()
                .contentType(uploadedImage.getContentType())
                .imageId(uploadedImage.getId())
                .width(uploadedImage.getWidth())
                .height(uploadedImage.getHeight())
                .rawUrl(url)
                .build();
    }

    @PostMapping("/batch")
    public UploadBatchRes uploadBatch(@RequestPart("files") List<MultipartFile> files,
                                      @RequestPart("slots") List<ImageSlot> slots){ // "FRONT","REAR","LEFT","RIGHT"
        if(files == null || files.isEmpty()) throw new IllegalArgumentException("파일이 없습니다.");
        if(files.size() != slots.size()) throw new IllegalArgumentException("슬롯의 개수에 맞게 업로드해주세요.");
        if(files.size() > 4) throw new IllegalArgumentException("최대 4장까지 업로드 가능합니다.");

        List<UploadedImage> out = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            ImageSlot slot = slots.get(i);
            Long imageId = imageService.upload(file).getId();
            out.add(new UploadedImage(slot, imageId));
        }

        return new UploadBatchRes(out);
    }

    public record UploadBatchRes(List<UploadedImage> images){}
    public record UploadedImage(ImageSlot slot, Long imageId){}



}
