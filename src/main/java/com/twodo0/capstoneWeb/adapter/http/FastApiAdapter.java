package com.twodo0.capstoneWeb.adapter.http;

import com.twodo0.capstoneWeb.config.AiProperties;
import com.twodo0.capstoneWeb.dto.FastApiPredictRes;
import com.twodo0.capstoneWeb.port.InferencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;
      import java.util.HashMap;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "ai.mock", havingValue = "false")
public class FastApiAdapter implements InferencePort {

    private final WebClient webClient;
    private final AiProperties props;

    @Override
    public FastApiPredictRes predictByUrl(String imageUrl, double yoloThreshold, double vitThreshold, String model, String previewUploadUrl) {
        return predictByUrl(imageUrl, yoloThreshold, vitThreshold, model, previewUploadUrl, null);
    }

    @Override
    public FastApiPredictRes predictByUrl(String imageUrl, double yoloThreshold, double vitThreshold, String model, String previewUploadUrl, String heatmapUploadUrl) {
        var req = new HashMap<String, Object>();
        req.put("raw_url", imageUrl);
        req.put("preview_url", previewUploadUrl);
        req.put("yoloThreshold", yoloThreshold);
        req.put("vitThreshold", vitThreshold);
        req.put("model", model);
        if(heatmapUploadUrl != null) {
            req.put("heatmap_put_url", heatmapUploadUrl);
        }

        //FastAPI한테 이 imageUrl 들고 가서 추론해달라고 POST 요청
        //FastAPI가 히트맵 생성 후 전달해준 url에 업로드함
        // 추론에 대한 응답을 FastApiPredictsRes로 역직렬화
        return webClient.post()
                .uri(props.endpoints().predict()) //WebClient 빈을 만들 때 baseUrl까지 넣어줌
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(h -> h.remove("Expect"))
                .bodyValue(req)
                .retrieve() // 응답 꺼낼 준비
                // FastAPI와의 HTTP 통신 결과에 대한 에러 처리(원격 호출 실패)
                .onStatus(HttpStatusCode::isError, r ->
                        r.bodyToMono(String.class).defaultIfEmpty("")
                                .map(msg -> new RuntimeException("FastAPI / predict error: " + r.statusCode() + ": " + msg))
                )
                .bodyToMono(FastApiPredictRes.class)
                .doOnError(e -> log.error("Decode failed", e))
                .block(Duration.ofSeconds(props.timeoutSeconds())); // 동기 대기(타임아웃 포함)
    }
}