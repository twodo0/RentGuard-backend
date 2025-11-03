# Inference API 계약서 (v1)

## 요청
- POST /predict
- Content-Type: multipart/form-data
  - image (required): JPG/PNG 1장, ≤10MB
  - threshold (optional, number, 0~1, default 0.5)
  - return (optional, array[string]): ["boxes","heatmap"] 등

## 응답 (성공: 200, application/json)
{
  "schema_version": "1.0.0",
  "model_version": "v0.0.1",
  "task": "detection",
  "image": { "width": 1280, "height": 720 },
  "preds": [
    { "label": "scratch", "score": 0.92, "box": [100,120,200,180] }
  ],
  "heatmap_url": null,
  "inference_ms": 850,
  "request_id": "uuid-optional"
}

## 에러 (예시)
400 Bad Request:
{"error":{"code":"BAD_REQUEST","message":"image is required"}}

500 Internal Server Error:
{"error":{"code":"INTERNAL","message":"unexpected error"}}

## 좌표/단위 규칙
- box: [x, y, w, h], 단위: 픽셀, 원점: 좌상단(0,0)
- 정수/실수 허용(표시는 프론트에서 반올림)
