package com.twodo0.capstoneWeb.storage;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

@Component
public class StorageKeyFactory {

    @NotNull
    public String getRawKey(MultipartFile file, String ct) {
        String yyyy = DateTimeFormatter.ofPattern("yyyy").format(OffsetDateTime.now());
        String MM = DateTimeFormatter.ofPattern("MM").format(OffsetDateTime.now());
        String ext = resolveExt(Objects.toString(file.getOriginalFilename(), ""), ct);
        String key = "raw/%s/%s/%s.%s".formatted(yyyy, MM, UUID.randomUUID(), ext);
        return key;
    }

    @NotNull
    public String getHeatmapKey(Long predictionId){
        String yyyy = DateTimeFormatter.ofPattern("yyyy").format(OffsetDateTime.now());
        String mm = DateTimeFormatter.ofPattern("MM").format(OffsetDateTime.now());
        String key = "heatmap/%s/%s/%d.png".formatted(yyyy, mm, predictionId);
        return key;
    }

    @NotNull
    public String getPreviewKey(Long predictionId){
        String yyyy = DateTimeFormatter.ofPattern("yyyy").format(OffsetDateTime.now());
        String mm = DateTimeFormatter.ofPattern("MM").format(OffsetDateTime.now());
        String key = "preview/%s/%s/%d.png".formatted(yyyy, mm, predictionId);
        return key;
    }

    private String resolveExt(String originalFileName, String contentType) {
        String ext = extFromFilename(originalFileName);
        if (ext != null) return ext;
        // content-type 기반 추정
        if ("image/jpeg".equalsIgnoreCase(contentType)) return "jpg";
        if ("image/png".equalsIgnoreCase(contentType)) return "png";
        if("image/webp".equalsIgnoreCase(contentType)) return "webp";
        return "bin";
    }

    private String extFromFilename(String name) {
        int i = name.lastIndexOf('.');
        if (i <= 0 || i == name.length() -1) {
            return null;
        }
        return name.substring(i + 1).toLowerCase();
    }

}
