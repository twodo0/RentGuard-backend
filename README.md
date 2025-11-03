# Capstone Web (MVP)

## 목표(한 줄)
웹에서 사진 1장을 업로드하면 **파손 유무/종류/위치**를 판별해 결과(JSON/박스 오버레이)를 화면에 표시한다.

## MVP 수용 기준
- 업로드: JPG/PNG, 1장, ≤10MB
- API: `POST /predict` → 응답에 `preds[].{label, score, box[4]}`
- 1차: 응답 JSON 화면 출력
- 2차: 이미지 위 박스 오버레이(`box=[x,y,w,h]`, 픽셀, 원점 좌상단)
- 오류 시 표준 에러 JSON(`error.code/message`) 노출

## 실행 (개발용)
```bash
# 웹 (Spring Boot, 이후 생성 예정)
cd web
./gradlew bootRun
# 브라우저: http://localhost:8080
