package com.twodo0.capstoneWeb.controller;

import com.twodo0.capstoneWeb.domain.ImageMeta;
import com.twodo0.capstoneWeb.dto.UploadResponseDto;
import com.twodo0.capstoneWeb.port.PresignUrlPort;
import com.twodo0.capstoneWeb.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

}
