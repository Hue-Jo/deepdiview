# 🍿영화에 대한 심도 있는 토론을 원한다면? Deepdiview! 
> TMDB의 오픈API를 활용하여 넷플릭스(KR) 제공 영화 정보를 검색하고, 투표, 감상 인증, 토론을 통해 다양한 의견을 나눌 수 있는 서비스


## 🎯 프로젝트 목적 
> 단순한 영화 리뷰를 넘어, 감상 인증을 거친 신뢰도 높은 영화 커뮤니티 구축
- 매주 투표로 ‘이 주의 영화’를 선정하고, 자유롭게 리뷰를 남길 수 있습니다. 
- 감상 인증을 거친 사용자들은 해당 영화에 대한 심도 깊은 토론에도 참여할 수 있습니다.
-----
## 🛠 사용 기술 스택
- 언어 : Java 17
- 프레임워크 : SpringBoot 3.4
- 데이터베이스 : MySQL, Redis(토큰, 캐시, 금지어 관리)
- 파일 저장 : AWS S3 (이미지 저장)
- ORM : Spring Data JPA
- 보안 : Spring Security, JWT (jjwt 0.12)
- 실시간 통신 : SSE
- 배포 환경 : AWS EC2 + NginX + Spring Boot, Github Actions(CI/CD)
- 프로토콜 : Https 지원

### 사용 오픈 API
- [TMDB_Netflix KR 영화정보](https://api.themoviedb.org/3/discover/movie?include_adult=true&include_video=false&language=ko&sort_by=primary_release_date.desc&watch_region=KR&with_watch_providers=8)

---
### ERD
![Image](https://github.com/user-attachments/assets/cdb5711d-e3ce-4821-8e6b-726b96b4d6cf)

<details>
  <summary> 상세 ERD </summary>
https://github.com/user-attachments/assets/d3e371d5-d93e-41f9-94f6-cf885f38626e
</details>

---

### 아키텍처 
![Image](https://github.com/user-attachments/assets/564eefb1-9389-471b-b6d9-075dd6716367)
  
## 📌 주요 기능 
### 1️⃣ 사용자
- 권한 : 일반 사용자(USER), 관리자(ADMIN)
- 이메일 인증
  - 회원가입 시, 사용자의 이메일로 6자리 인증 코드를 전송하여 본인 확인
    - JavaMailSender의 MimeMessage를 사용하여 HTML 형식의 이메일 전송
  - 사용자는 5분 내에 전송받은 코드를 입력하여 본인 인증
  - 본인 인증 후 10분 내로 회원가입 하지 않을 시, 가입 반려
  - 회원가입 완료 시,  해당 이메일의 인증정보는 Redis에서 자동 삭제
- 로그인
  - JWT 기반 Access Token + Refresh Token 발급 
  - Refresh Token은 Redis에 저장되며 15일간 유효
  - Refresh Token이 없거나 만료된 경우 새로 발급 후 Redis에 저장
- 로그아웃 
  - Redis에서 Refresh Token 삭제
  - 사용 중이던 Access Token은 만료 시간까지 Redis에 블랙리스트로 등록
  - SecurityContext 초기화로 세션 종료
- 접근 권한 
  - 로그인한 사용자 : 모든 주요 기능 사용 가능
  - 비로그인 사용자 : 영화 조회 및 리뷰/댓글 열람만 가능
### 2️⃣ TMDB API를 활용한 영화정보 제공
-  매주 일요일 0시, 넷플릭스(KR) 영화 데이터 자동 업데이트 (스케줄링)
   -  Netflix KR에서 더 이상 제공하지 않는 영화는 DB에서 삭제되지 않고 상태만 변경됨. 
- 영화 제목 및 키워드 검색 지원
  - 띄어쓰기 무시 가능 (ex. 다크나이트 = 다크 나이트 = thedarkknight = dark knight)
- 자주 조회되는 인기 Top 20 영화는 Redis 캐시에 저장하여 빠르게 조회 가능
  - 매주 일요일 0시, 캐시 초기화
- 서비스 내에서 받은 평균별점, 별점 분포도 함께 제공됨
- 영화 상세 조회 시, 최근 리뷰 및 사용자가 작성한 리뷰도 함께 제공 
### 3️⃣ 영화 투표 (매주 진행)
- 투표 대상 : 넷플릭스 인기 TOP 6 영화
  - 이전에 1위를 했던 영화는 이후의 투표 대상에서는 제외됨
- 생성 (관리자 권한)
  - 매주 일요일에만 생성 가능
- 참여 (일반 사용자)
  - 투표 기간 : 월요일-토요일 
  - 투표 방법 : 일주일에 1회 참여 가능, 한 번의 투표에서 하나의 영화만 선택 가능
  - 투표 후 결과 즉시 확인 가능
- 동률 발생 시
  - 마지막에 득표한 영화가 상위 랭크 차지 
- 자주 조회되는 지난주 투표 1위 영화는 Redis 캐시에 저장하여 빠르게 조회 가능  
  - 매주 일요일 0시, 캐시 초기화
- 투표에 참여한 사용자가 회원탈퇴를 해도 투표 결과 보존

### 4️⃣ 영화 감상 인증
- 지난주 투표에서 1위를 한 영화에 대한 감상 인증
- 인증 기간 : 월-토
- 인증 방식
  - 사용자 : 인증샷 업로드
  - 관리자 : 업로드된 사진 확인 후 승인/거절(거절 사유 필수 포함)
    - S3에 업로드 예시 사진을 저장하여 사용자에게 제공
    - 인증 요청 조회는 커서 기반 페이징 처리 
  - 승인을 받은 사용자만 해당 영화에 대한 토론 참여 가능
- 기타 규칙
  - 이미 승인을 받은 경우, 중복 인증과 인증 수정/삭제 불가 
  - 매주 일요일 0시, 인증 상태 초기화 (스케줄링)
- 상태 : PENDING(요청), APPROVED(승인), REJECTED(거절, 사유 포함)
- SSE를 통해 인증 승인/거절 알림 전송
 
### 5️⃣ 리뷰/댓글
- 리뷰(일반) : 특정 영화에 대해 한 번만 작성 가능
  - 최신수, 좋아요 수 등 정렬하여 조회 가능  
- 댓글 : 특정 리뷰에 대해 여러 개 작성 가능
  - 댓글 목록 조회시, 커서 기반으로 페이징 처리
- 욕설 필터링 : 리뷰/댓글에 포함된 욕설은 *로 마스킹 처리됨
  - 필터링에 사용되는 금지어 목록은 Redis에 저장되어 관리됨
- SSE를 통해 리뷰에 댓글이 달릴 시 알림 전송 
### 6️⃣ 좋아요
- 특정 리뷰에 대해 한 번만 가능
  - 두 번 누를 시 취소
- 좋아요 순 리뷰 정렬 기능 제공
- SSE를 통해 리뷰에 좋아요가 눌릴 시 알림 전송 

### 7️⃣ 토론 (인증 리뷰)
- 인증 승인을 받은 사용자만 작성할 수 있는 토론 게시판 제공 
- 리뷰 조회시, 인증된 리뷰만 필터링하여 조회 가능
- 토론 게시판에서 작성된 리뷰에는 '인증된 리뷰' 표시 부착

### 8️⃣ 알림 (SSE 기반)
- 발생 조건  
  - 리뷰에 댓글이 달릴 때
  - 리뷰에 좋아요가 눌릴 때
  - 인증 상태가 바뀔 때 (APPROVED, REJECTED)
- 본인의 행동에 대해서는 알림이 가지 않음
  - 타인에 의해서만 알림 작동
- 특정/모든 알림 읽음처리 가능
- 30일 전에 받은 알림들만 조회 가능
  - 매일 오전 3시, 31일 이전에 받은 알림은 DB에서 삭제 스케줄링
- 알림 목록은 커서 기반으로 페이징처리  
- 알림 목록 조회 중에 새로운 알림이 발생해도 실시간으로 내용 확인 가능 

### 📅 주간 일정, 스케줄링 요약

| 요일        | 기능                |
|-------------|---------------------|
| 일요일      | - 인기 영화 DB 업데이트 (TMDB)<br>- 캐시 초기화<br>- 인증 상태 초기화<br>- 새로운 투표 생성 (관리자) |
| 월~토       | - 사용자 투표 참여<br>- 감상 인증 제출 |
| 월~토 (관리자) | - 인증 요청 검토 및 승인/거절 |
| 매일 오전 3시 | - 31일 이상 지난 알림 자동 삭제 |
| 수시        | - 알림 수신 |
-----

## 🌐 배포 주소
- https://deepdiview.vercel.app
- API 명세서 : [Swagger](https://deepdiview.site/swagger-ui/index.html)



