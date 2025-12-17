# CarGuard-backend

차량 대여/반납 과정에서 차량 손상을 자동으로 탐지하고,
렌탈 세션 단위로 손상 이력을 관리하는 Spring Boot 기반 백엔드 애플리케이션입니다.
	•	차량 렌탈 세션 관리
	•	단일 / 4장 배치 이미지 업로드
	•	YOLO → ViT 기반 손상 탐지 (비동기 처리)
	•	렌트 시작/반납 시 손상 요약 및 추가 손상 계산

⸻

1. 주요 기능

1.1 이미지 업로드
	•	POST /api/images
	•	단일 이미지 업로드
	•	MinIO에 저장 후 imageId, rawUrl, width, height 반환
	•	POST /api/images/batch
	•	최대 4장 이미지 배치 업로드
	•	요청:
	•	files: 이미지 리스트 (1~4장)
	•	slots: 촬영 위치 (FRONT, REAR, LEFT, RIGHT)
	•	응답:
	•	images: [{ slot, imageId }]
	•	이후 렌트 시작/종료 배치 API에서 사용

⸻

1.2 단일 이미지 손상 탐지 (비동기)
	•	POST /api/predictions/by-image/{imageId}
	•	단일 이미지에 대해 외부 FastAPI 서버로 손상 탐지 요청
	•	쿼리 파라미터:
	•	yoloThreshold (선택)
	•	응답:
	•	jobId, status를 포함한 비동기 작업 정보
	•	GET /api/predictions/jobs/{jobId}
	•	손상 탐지 작업 상태 조회
	•	응답:
	•	진행 중: 202 Accepted
	•	실패: 422 Unprocessable Entity
	•	완료: 200 OK + 탐지 결과
	•	bounding box 좌표
	•	손상 클래스별 확률
	•	이미지 원본 크기 정보

⸻

1.3 렌트 시작 / 종료 (4장 배치 처리)

렌트 시작 및 반납 시 앞/뒤/좌/우 4장 이미지를 기준으로
세션 단위의 손상 이력을 관리합니다.

1.3.1 렌트 시작
	•	POST /api/rentals/batch/start/upload
	•	요청:
	•	vehicleNo
	•	yoloThreshold
	•	images: [{ slot, imageId }]
	•	처리:
	•	각 이미지에 대해 손상 탐지 수행
	•	손상 타입별 개수 집계 (startSummary)
	•	세션 전체 손상 개수(startTotal) 계산
	•	렌탈 세션 생성 (IN_RENT)
	•	응답:
	•	rentalId
	•	startSummary
	•	startTotal

1.3.2 렌트 종료 (반납)
	•	POST /api/rentals/batch/end/upload
	•	요청:
	•	rentalId
	•	vehicleNo
	•	yoloThreshold
	•	images: [{ slot, imageId }]
	•	처리:
	•	반납 시 손상 요약(finishSummary) 계산
	•	렌트 시작 대비 증가분(deltaSummary) 산출
	•	추가 손상 개수(newDamageTotal) 계산
	•	세션 상태 RETURNED로 변경
	•	응답:
	•	finishSummary
	•	deltaSummary
	•	newDamageTotal

⸻

1.4 렌탈 세션 조회
	•	GET /api/rentals/recent
	•	최근 렌탈 세션 목록 조회 (페이지네이션)
	•	주요 필드:
	•	rentalId, vehicleNo, status
	•	startedAt, finishedAt
	•	newDamageTotal
	•	GET /api/rentals/{rentalId}
	•	단일 렌탈 세션 상세 조회
	•	포함 정보:
	•	렌트 시작/종료 손상 요약
	•	추가 손상 개수
	•	이미지별 탐지 결과 (bbox + 손상 확률)

⸻

2. 아키텍처 개요

2.1 구성 요소
	•	Frontend: React (별도 레포지토리)
	•	Backend: Spring Boot
	•	Inference: FastAPI + YOLO + ViT (별도 레포지토리)
	•	Storage: MinIO (S3 호환)
	•	Database: MySQL

2.2 처리 흐름
	1.	프론트에서 이미지 업로드 → Spring → MinIO 저장
	2.	필요 시 단일 이미지 손상 탐지 요청 (비동기)
	3.	렌트 시작:
	•	4장 이미지 기준 손상 요약 저장
	4.	렌트 종료:
	•	시작 대비 추가 손상 계산
	5.	렌탈 세션 목록 및 상세 조회

⸻

3. 기술 스택
	•	Java 17
	•	Spring Boot
	•	Spring Web
	•	Spring Data JPA
	•	MySQL
	•	MinIO (S3 호환 스토리지)
	•	FastAPI (외부 추론 서버)

⸻

4. 도메인 모델 요약

RentalSession
	•	차량 렌탈 단위 세션
	•	상태: IN_RENT, RETURNED
	•	손상 요약 정보 및 이미지 연관 관계 보유

RentalImage
	•	렌트 시작/종료 시 촬영된 이미지
	•	위치(FRONT, REAR, LEFT, RIGHT) 기준 관리
	•	손상 탐지 결과와 연결

Prediction / PredictionJob
	•	이미지 손상 탐지 결과 및 비동기 작업 관리
	•	bounding box, 손상 확률 정보 포함

공통 Enum
	•	DamageType: 손상 유형
	•	RentalStatus: 세션 상태
	•	Phase: 렌트 시작/종료
	•	ImageSlot: 촬영 위치
