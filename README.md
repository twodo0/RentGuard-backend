# CarGuard-backend

차량 대여/반납 과정에서 차량 손상을 자동으로 탐지하고 이력을 관리하는 Spring Boot 백엔드 애플리케이션이다.  

- 차량 렌탈 세션 관리
- 이미지 업로드 (단일/4장 배치)
- YOLO → ViT 기반 손상 탐지 (비동기)
- Grad-CAM 히트맵 생성(FastAPI 연동, 단일 탐지용)
- 렌트 시작/반납 시 손상 요약 및 추가 손상 계산

을 담당한다.

---

## 1. 주요 기능

### 1.1 이미지 업로드

- `POST /api/images`
  - 단일 이미지 업로드
  - MinIO에 저장 후 `imageId`, `rawUrl`, `width`, `height` 등을 반환

- `POST /api/images/batch`
  - 4장 이미지 배치 업로드
  - 요청 바디:
    - `files`: 이미지 리스트 (`MultipartFile` 1~4장)
    - `slots`: 각 이미지에 대응하는 촬영 위치 (`FRONT`, `REAR`, `LEFT`, `RIGHT`)
  - 응답:
    - `images: [{ slot, imageId }]`  
      → 이후 렌트 시작/종료 배치 API 호출 시 사용

### 1.2 손상 탐지 (단일 이미지 비동기 추론)

- `POST /api/predictions/by-image/{imageId}`
  - 지정한 `imageId`에 대해 FastAPI YOLO→ViT 파이프라인에 **비동기 추론** 요청
  - 쿼리 파라미터:
    - `yoloThreshold` (선택, 미지정 시 기본값 사용)
  - 응답: `PredictionJobDto`
    - `jobId`, `status` 등

- `GET /api/predictions/jobs/{jobId}`
  - 특정 `jobId`에 대한 추론 상태 및 최종 결과 조회
  - 응답:
    - 진행 중: `202 Accepted` + `Retry-After` 헤더
    - 실패: `422 Unprocessable Entity` + 에러 메시지
    - 완료: `200 OK` + `PredictionDetailDto`
      - `predictionId`
      - `rawUrl`, `heatmapUrl`
      - `detections` (bbox + class_probs)
      - 원본 이미지 크기 (`width`, `height`)

> 이 단일 예측 API는 **우측 상단 메뉴에서 사용하는 “단일 손상 탐지 / 히트맵 데모 화면”**에서 활용된다.

### 1.3 Grad-CAM 히트맵 저장 (단일 탐지용)

- FastAPI 서버가 YOLO→ViT 추론과 함께 Grad-CAM 히트맵 PNG 생성
- MinIO의 전용 heatmap 버킷에 저장
- Spring에서 `heatmapBucket`, `heatmapKey`를 관리하고,
  프론트에는 서명 URL(`heatmapUrl`)로 제공하여 이미지 위에 오버레이할 수 있게 한다.

### 1.4 차량 렌탈 시작 / 종료 (4장 배치 플로우 – 메인 기능)

렌트 시작/반납 시 **앞/뒤/좌/우 4장**을 한 번에 업로드해 세션 단위로 손상을 관리한다.  
(프론트에서는 먼저 `/api/images/batch`로 이미지를 업로드하여 `imageId`들을 받은 뒤,  
해당 `imageId` 리스트를 들고 아래 배치 API를 호출한다.)

#### 1.4.1 렌트 시작 배치

- `POST /api/rentals/batch/start/upload`
- 요청:
  - `vehicleNo` (차량 번호)
  - `yoloThreshold` (탐지 임계치)
  - `images`: `[ { slot: FRONT|REAR|LEFT|RIGHT, imageId } ]`
- 처리:
  - 각 이미지에 대해 손상 탐지 수행
  - 라벨별 손상 개수를 집계해 **렌트 시작 시 손상 요약 (`startSummary`)** 생성
  - 세션 전체 손상 개수(`startTotal`) 계산
  - `RentalSession` 생성 (`status = IN_RENT`)
- 응답: `RentalStartBatchRes`
  - `rentalId`
  - `vehicleNo`
  - `startSummary: Map<DamageType, Integer>`
  - `startTotal: Integer`

#### 1.4.2 렌트 종료(반납) 배치

- `POST /api/rentals/batch/end/upload`
- 요청:
  - `rentalId` (기존 세션 ID)
  - `vehicleNo` (검증용)
  - `yoloThreshold`
  - `images`: `[ { slot: FRONT|REAR|LEFT|RIGHT, imageId } ]`
- 처리:
  - 각 종료 이미지에 대해 손상 탐지 수행
  - 라벨별 손상 개수를 집계해 **반납 시 손상 요약 (`finishSummary`)** 생성
  - 렌트 시작 시점과 비교하여 라벨별 차이(`deltaSummary`) 계산
  - `delta > 0`인 라벨만 **추가 손상**으로 집계해 `newDamageTotal` 계산
  - 세션 상태를 `RETURNED`로 변경
- 응답: `RentalFinishBatchRes`
  - `rentalId`
  - `vehicleNo`
  - `finishSummary: Map<DamageType, Integer>`
  - `deltaSummary: Map<DamageType, Integer>`
  - `newDamageTotal: Integer`

### 1.5 렌탈 세션 조회

- `GET /api/rentals/recent`
  - 최근 렌탈 세션 목록 조회 (페이지네이션)
  - 각 Row:
    - `rentalId`
    - `vehicleNo`
    - `status` (`IN_RENT`, `RETURNED`)
    - `startedAt`, `finishedAt`
    - `newDamageTotal` (반납 완료 시 추가 손상 개수)

- `GET /api/rentals/{rentalId}?phase=START|END(optional)`
  - 단일 렌탈 세션 상세 조회
  - 응답: `RentalDetailDto`
    - 메타:
      - `rentalId`, `vehicleNo`, `status`, `startedAt`, `finishedAt`
    - 세션 요약:
      - `startSummary`, `finishSummary`, `deltaSummary`
      - `startTotal`, `finishTotal`, `newDamageTotal`
    - 이미지:
      - `startImages: List<RentalImageDto>`
      - `finishImages: List<RentalImageDto>`
        - `IN_RENT` 상태인 경우 비어 있을 수 있음
      - 각 `RentalImageDto`:
        - `slot` (FRONT/REAR/LEFT/RIGHT)
        - `rawUrl`
        - `heatmapUrl` (필요 시)
        - `summaryByImage: Map<DamageType, Integer>`
        - `detections: List<DetectionDto>` (bbox + class_probs)  
          → 프론트에서 bbox와 확률 툴팁 렌더링에 사용

---

## 2. 아키텍처 개요

### 2.1 전체 구성

- Frontend: React + Vite (별도 레포지토리)
- Backend: Spring Boot (본 레포지토리)
- Inference: FastAPI + YOLO + ViT (별도 레포지토리)
- Storage: MinIO (S3 호환 스토리지)

### 2.2 주요 흐름

1. 프론트에서 차량 이미지 업로드  
   - `/api/images` 또는 `/api/images/batch` → Spring → MinIO 저장 → `imageId` 반환
2. 필요 시 단일 이미지 비동기 예측  
   - `/api/predictions/by-image/{imageId}` → FastAPI → YOLO→ViT 추론 + Grad-CAM 생성  
   - 결과/히트맵을 MinIO에 저장하고 `/api/predictions/jobs/{jobId}`로 조회
3. 렌트 시작:
   - 4장 이미지 업로드 후 `/api/rentals/batch/start/upload` 호출
   - 시작 시 손상 요약(`startSummary`, `startTotal`) 저장
4. 렌트 종료:
   - 동일 기준 4장 업로드 후 `/api/rentals/batch/end/upload` 호출
   - 반납 시 손상 요약(`finishSummary`, `finishTotal`) 및 `deltaSummary`, `newDamageTotal` 계산
   - 세션 상태를 `RETURNED`로 변경
5. 조회:
   - `/api/rentals/recent`로 최근 세션 목록 조회
   - `/api/rentals/{rentalId}`에서 세션 요약 + 이미지별 bbox/확률 정보 조회

---

## 3. 기술 스택

- Java 17
- Spring Boot
  - Spring Web
  - Spring Data JPA
- MinIO (S3 호환 객체 스토리지)
- FastAPI (외부 추론 서버, Python)
- Database
  - MySQL

---

## 4. 도메인 모델 개요 (요약)

### 4.1 Prediction

- 단일 이미지에 대한 손상 탐지 결과
- 주요 필드:
  - `id`
  - `image` (버킷/키, width/height)
  - `heatmapBucket`, `heatmapKey`
  - `detections` (bbox, classProbs 등)

### 4.2 PredictionJob

- 비동기 추론 작업
- 주요 필드:
  - `id`
  - `status` (QUEUED, RUNNING, SUCCEEDED, FAILED 등)
  - `prediction` 연관관계
  - 오류 메시지 등

### 4.3 RentalSession

- 차량 한 대에 대한 렌탈 세션
- 주요 필드:
  - `id` (`rentalId`)
  - `vehicleNo`
  - `status` (`IN_RENT`, `RETURNED`)
  - `startSummaryJson`, `endSummaryJson`, `deltaJson`
  - `images` (각 phase/slot별 `RentalImage` 리스트)

### 4.4 RentalImage

- 렌트 시작/반납 시 촬영한 단일 이미지
- 주요 필드:
  - `phase` (`START`, `END`)
  - `slot` (`FRONT`, `REAR`, `LEFT`, `RIGHT`)
  - `prediction` 연관관계

### 4.5 DamageType / RentalStatus / Phase / ImageSlot

- `DamageType` : 손상 라벨 (예: `BREAKAGE`, `CRUSHED`, `SCRATCHED`, `SEPARATED`, `NORMAL` 등)
- `RentalStatus` : 세션 상태 (`IN_RENT`, `RETURNED`)
- `Phase` : 렌트 시작/종료 구분 (`START`, `END`)
- `ImageSlot` : 촬영 위치 (`FRONT`, `REAR`, `LEFT`, `RIGHT`)
