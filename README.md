# CarGuard-backend

차량 대여/반납 과정에서 차량 손상을 자동으로 탐지하고 이력을 관리하는 Spring Boot 백엔드 애플리케이션이다.  
차량 렌탈 세션 관리, 이미지 업로드, 손상 탐지, Grad-CAM 히트맵 생성(FastAPI 호출)까지를 담당한다.

---

## 1. 주요 기능

### 1.1 이미지 업로드

- `POST /api/images`
- 프론트에서 업로드한 차량 이미지를 MinIO에 저장한다.
- 원본 이미지의 접근 URL과 내부 식별자(`imageId`)를 반환한다.

### 1.2 손상 탐지 (비동기 추론)

- `POST /api/predictions/by-image/{imageId}`
- 지정한 `imageId`에 대해 FastAPI YOLO→ViT 파이프라인에 비동기 추론을 요청한다.
- 바로 `PredictionJob` 정보를 담은 DTO를 반환하고, 실제 추론은 백그라운드에서 수행한다.

- `GET /api/predictions/jobs/{jobId}`
- 특정 `jobId`에 대한 추론 상태 및 최종 결과를 조회한다.
-  
  - 진행 중: `202 Accepted`
  - 완료: `200 OK` + `PredictionDetail` 응답

### 1.3 Grad-CAM 히트맵 저장

- FastAPI 서버가 추론 결과와 함께 Grad-CAM 히트맵 이미지를 생성한다.
- MinIO에 `heatmaps` 버킷으로 저장하고, 서명된 URL을 통해 프론트에서 오버레이로 표시한다.

### 1.4 차량 렌탈 시작 / 종료

- `POST /api/rentals/start/upload`
  - 쿼리 파라미터: `imageId`, `vehicleNo`, `yoloThreshold`, `vitThreshold`, `model`
  - 해당 이미지로 손상 탐지를 수행한 뒤, 결과를 요약하여 `RentalSession`을 생성한다.
  - 시작 시 손상 요약(`ConditionSummary`)을 JSON으로 세션에 저장한다.
  - 응답은 `RentalStartRes`:
    - `rentalSessionId`
    - `predictionId`
    - `start.total`, `start.byClass`
    - `rentalStatus` (예: `IN_RENT`)

- `POST /api/rentals/end/upload`
  - 쿼리 파라미터: `rentalId`, `imageId`, `vehicleNo`, `yoloThreshold`, `vitThreshold`, `model`
  - 진행 중(`IN_RENT`)이던 세션을 로드하고, 반납 시 이미지를 기반으로 다시 손상 탐지를 수행한다.
  - 시작/종료 손상 요약을 비교하여 `delta(손상 증감)`를 계산한다.
  - 응답은 `RentalFinishRes`:
    - `rentalSessionId`
    - `predictionId`
    - `finish.total`, `finish.byClass`
    - `delta[DamageType]` (증가/감소 개수)
    - `rentalStatus` (예: `RETURNED`)

---

## 2. 아키텍처 개요

### 2.1 전체 구성

- Frontend: React + Vite (별도 레포지토리, `CarGuard-frontend`)
- Backend: Spring Boot (본 레포지토리)
- Inference: FastAPI + YOLO + ViT (별도 레포지토리)
- Storage: MinIO (S3 호환 스토리지)

### 2.2 주요 흐름

1. 프론트에서 차량 이미지 업로드 → Spring → MinIO 저장 → `imageId` 반환
2. 프론트에서 `imageId`로 비동기 추론 요청 → Spring → FastAPI 호출
3. FastAPI에서 YOLO로 박스 검출 → ViT로 박스별 손상 분류 → Grad-CAM 생성
4. FastAPI에서 MinIO에 preview, heatmap 업로드 → Spring에서 결과 저장
5. 렌탈 시작 시:
   - 시작 이미지로 추론 실행
   - 손상 요약(`ConditionSummary`) 저장
6. 렌탈 종료 시:
   - 종료 이미지로 재추론
   - 시작/종료 요약 비교 → `delta` 계산
   - 반납 상태로 세션 종료

---

## 3. 기술 스택

- Java 17
- Spring Boot
  - Spring Web
  - Spring Data JPA
  - Validation
- Gradle
- MinIO (S3 호환 객체 스토리지)
- FastAPI (외부 추론 서버, Python)
- Database
  - MySQL

---

## 4. 도메인 모델 개요

### 4.1 Prediction

- 단일 이미지에 대한 손상 탐지 결과를 나타낸다.
- 주요 필드 예:
  - `id`
  - `imageId`
  - `rawUrl` (MinIO 원본 이미지 경로/URL)
  - `heatMapUrl`
  - `detections` (박스, 클래스 확률 등)

### 4.2 PredictionJob

- 비동기 추론 작업을 표현한다.
- 주요 필드 예:
  - `id`
  - `status` (PENDING, RUNNING, DONE, FAILED 등)
  - `prediction` 연관관계
  - 오류 메시지 등

### 4.3 RentalSession

- 차량 한 대에 대한 렌탈 세션을 나타낸다.
- 주요 필드 예:
  - `id` (rentalSessionId)
  - `vehicleNo` (차량 번호)
  - `status` (`RentalStatus`: `IN_RENT`, `RETURNED` 등)
  - `startPrediction`, `endPrediction`
  - `startSummaryJson` (시작 시 `ConditionSummary` JSON)
  - `deltaJson` (손상 증감 JSON)

### 4.4 DamageType / RentalStatus

- `DamageType`
  - `SCRATCH`, `DENT`, `BREAKAGE`, `SEPARATION`, …  
  (프로젝트 정의에 따라 변할 수 있다)
- `RentalStatus`
  - `IN_RENT`, `RETURNED` 등

---

## 5. 주요 API 요약

자세한 스키마는 추후 Swagger/OpenAPI 문서로 정리한다는 가정으로, 여기서는 엔드포인트 개요만 적는다.

### 5.1 이미지 업로드

- `POST /api/images`
- `multipart/form-data`로 이미지 파일을 업로드한다.
- 응답: `imageId`, `rawUrl` 등

### 5.2 예측 (비동기)

- `POST /api/predictions/by-image/{imageId}`
  - 쿼리 파라미터:
    - `yoloThreshold` (선택, 기본값 존재)
    - `vitThreshold` (선택, 기본값 존재)
    - `model` (선택, 예: `YOLO - ViT`)
  - 응답: `PredictionJobDto` (jobId, 상태 등)

- `GET /api/predictions/jobs/{jobId}`
  - 진행 중인 경우: HTTP 202
  - 완료된 경우: HTTP 200 + `PredictionDetail`

### 5.3 렌탈 시작

- `POST /api/rentals/start/upload`
  - 쿼리 파라미터:
    - `imageId` (필수)
    - `vehicleNo` (필수)
    - `yoloThreshold` (선택, 기본값 0.2 등)
    - `vitThreshold` (선택, 기본값 0.65 등)
    - `model` (선택, 기본값 `YOLO - ViT`)
  - 응답: `RentalStartRes`
    - `rentalSessionId`
    - `vehicleNo`
    - `predictionId`
    - `start.total`, `start.byClass`
    - `rentalStatus`

### 5.4 렌탈 종료

- `POST /api/rentals/end/upload`
  - 쿼리 파라미터:
    - `rentalId` (필수)
    - `imageId` (필수)
    - `vehicleNo` (필수, 검증용)
    - `yoloThreshold` (선택)
    - `vitThreshold` (선택)
    - `model` (선택)
  - 응답: `RentalFinishRes`
    - `rentalSessionId`
    - `vehicleNo`
    - `predictionId`
    - `finish.total`, `finish.byClass`
    - `delta[DamageType]`
    - `rentalStatus`

