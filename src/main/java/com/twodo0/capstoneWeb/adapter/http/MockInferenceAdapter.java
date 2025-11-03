package com.twodo0.capstoneWeb.adapter.http;

import com.twodo0.capstoneWeb.domain.enums.DamageType;
import com.twodo0.capstoneWeb.dto.FastApiPredictRes;
import com.twodo0.capstoneWeb.port.InferencePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(value = "ai.mock", havingValue = "true", matchIfMissing = true) // 해당 프로퍼티가 아예 설정되어 있지 않아도 조건을 만족한 것으로 간주
public class MockInferenceAdapter implements InferencePort {

    @Override
    public FastApiPredictRes predictByUrl(String imageUrl, double threshold, String model) {
        return predictByUrl(imageUrl, threshold, model, null);
    }

    @Override
    public FastApiPredictRes predictByUrl(String imageUrl, double threshold, String model, String heatmapUploadUrl){

        // FastApiPredictRes의 필드들 인위적으로 만들기
        // 더미 박스,확률
        List<FastApiPredictRes.BoxDto> boxes = List.of(
                new FastApiPredictRes.BoxDto(
                        List.of(
                                new FastApiPredictRes.ApiClassProbDto(DamageType.CAR_DAMAGE, 0.80),
                                new FastApiPredictRes.ApiClassProbDto(DamageType.DENT, 0.20),
                                new FastApiPredictRes.ApiClassProbDto(DamageType.GLASS_BREAK, 0.00),
                                new FastApiPredictRes.ApiClassProbDto(DamageType.SCRATCH, 0.00)
                        ),
                        100.0, 80.0, 160.0, 90.0
                ),
                new FastApiPredictRes.BoxDto(
                        List.of(
                                new FastApiPredictRes.ApiClassProbDto(DamageType.DENT, 0.72),
                                new FastApiPredictRes.ApiClassProbDto(DamageType.SCRATCH, 0.28),
                                new FastApiPredictRes.ApiClassProbDto(DamageType.CAR_DAMAGE, 0.00),
                                new FastApiPredictRes.ApiClassProbDto(DamageType.GLASS_BREAK, 0.00)
                        ),
                        420.0, 200.0, 120.0, 110.0
                )

        );

        // presigned PUT URL이 오면, 더비 PNG를 거기로 업로드 (MinIO에 실제로 저장)
        if(heatmapUploadUrl != null && !heatmapUploadUrl.isEmpty()){
            try {
                byte[] png = loadMockHeatmap(); // 클래스패스에서 읽거나 폴백 생성
                // Java 11 HttpClient로 간단 PUT
                var client = HttpClient.newHttpClient();
                var req = HttpRequest.newBuilder(URI.create(heatmapUploadUrl))
                        .header("Content-Type", "image/png") // 서명에 포함될 수 있으니 꼭 일치
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(png))
                        .build();
                var resp = client.send(req, HttpResponse.BodyHandlers.discarding());
                if (resp.statusCode() / 100 != 2) {
                    log.warn("Mock heatmap upload failed, status code {}", resp.statusCode());
                } else {
                    log.info("Mock heatmap upload success, status code {}", resp.statusCode());
                }
            } catch (Exception e) {
                log.warn("Mock heatmap upload error: {}", e.toString());
            }
        }

        return new FastApiPredictRes(
                "yolo-resnet-mock",
//                new FastApiPredictRes.ImageInfo(1280, 720),
                threshold,
                boxes
        );

    }
    private byte[] loadMockHeatmap() {
        try (var in = new ClassPathResource("mock/heatmap.png").getInputStream();
                var baos = new ByteArrayOutputStream()) {
            in.transferTo(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            // 폴백 : 간단 PNG 바이트 생성
            try (var baos = new  ByteArrayOutputStream()) {
                var img = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
                var g = img.createGraphics();
                try {
                    g.setPaint(new GradientPaint(0, 0, new Color(255, 0, 0, 90), 512, 512, new Color(255, 255, 0, 90)));
                    g.fillRect(0, 0, 512, 512);
                    g.setColor(new Color(0, 0, 0, 160));
                    g.setFont(new Font("Dialog", Font.BOLD, 20));
                    g.drawString("MOCK HEATMAP", 130, 260);
                } finally {g.dispose();}
                ImageIO.write(img, "png", baos);
                return baos.toByteArray();
            } catch (Exception ex) {
                log.error("fallback heatmap build failed : {}", ex.toString());
                return "MOCK HEATMAP".getBytes();
            }
        }
    }

}
