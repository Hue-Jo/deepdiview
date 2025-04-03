# 🍿영화에 대한 심도 있는 토론을 원한다면? Deepdiview! 
> TMDB의 오픈API를 활용하여 넷플릭스(KR) 제공 영화 정보를 검색하고, 투표, 감상인증, 토론을 통해 다양한 의견을 나눌 수 있는 서비스

매주 투표를 통해 '이 주의 영화'를 선정하고, 감상 인증을 거쳐 해당 영화에 대한 깊이있는 토론을 진행할 수 있습니다. 

-----
## 사용 기술 스택
- 언어 : Java (JDK 17)
- 프레임워크 : SpringBoot 3.4
- 데이터베이스 : MySQL, Redis
- 파일 저장 : AWS S3
- ORM : Spring Data JPA
- 보안 : Spring Security, JWT (jjwt 0.12)
- 실시간 통신 : SSE
- 배포 환경 : AWS EC2 + NginX + Spring Boot, Github Actions(CI/CD)
- 프로토콜 : Https 지원
- API 문서화 : Swagger

### 사용 오픈 API
- [TMDB](https://api.themoviedb.org/3/discover/movie?include_adult=true&include_video=false&language=ko&sort_by=primary_release_date.desc&watch_region=KR&with_watch_providers=8)

### ERD
![Image](https://github.com/user-attachments/assets/cdb5711d-e3ce-4821-8e6b-726b96b4d6cf)

<details>
  <summary> 상세 ERD </summary>
https://github.com/user-attachments/assets/d3e371d5-d93e-41f9-94f6-cf885f38626e
</details>

---

## 🎯 프로젝트 목적 
- 단순한 영화 리뷰를 넘어, 감상 인증을 거친 신뢰도 높은 영화 커뮤니티 구축
- 투표, 인증, 리뷰(토론)을 통해 영화에 대한 깊이 있는 상호작용 유도 
---
  
## 📌 주요 기능 
### 1️⃣ 사용자
- 권한 : 일반 사용자(USER), 관리자(ADMIN)
- 인증 : JWT 기반 로그인 (Access Token + Refresh Token)
  - Refresh Token은 Redis에 저장하여 관리
- 회원가입 및 로그인 후, 인증된 사용자만 특정 기능 사용가능
  - 비로그인 : 영화 조회, 리뷰/댓글 조회만 가능  
### 2️⃣ TMDB API를 활용한 영화정보 제공
-  매주 월요일 0시, 넷플릭스(KR) 영화 데이터 자동 업데이트 (스케줄링)
   -  더 이상 제공되지 않는 영화는 DB에서 삭제되지 않고 상태만 변경됨 
- 영화 제목 및 키워드 검색 지원
  - 띄어쓰기 무시 가능 (ex. 다크나이트 = 다크 나이트)
- 자주 조회되는 인기 Top 20 영화는 Redis 캐시에 저장하여 빠르게 조회 가능
  - 매주 월요일 0시, 캐시 초기화  
### 3️⃣ 영화 투표 (매주 진행)
- 투표 대상 : 넷플릭스 인기 TOP 5 영화
- 생성 (관리자 권한)
  - 매주 일요일에만 생성 가능
- 참여 (일반 사용자)
  - 월요일~토요일 중 한 번만 참여 가능
  - 한 번의 투표에서 하나의 영화만 선택 가능
  - 투표 후 결과 즉시 확인 가능
- 동률 발생 시
  - 마지막에 득표한 영화가 상위 랭크 차지 
- 자주 조회되는 지난주 투표 1위 영화는 Redis 캐시에 저장하여 빠르게 조회 가능  
  - 매주 일요일 0시, 캐시 초기화

### 4️⃣ 영화 감상 인증
- 지난주 투표에서 1위를 한 영화에 대한 감상 인증
- 인증 방식
  - 사용자 : 인증샷 업로드
  - 관리자 : 업로드된 사진 확인 후 승인/거절(사유포함)
  - 승인을 받은 사용자만 해당 영화에 대한 토론 참여 가능
- 기타 규칙
  - 이미 승인을 받은 경우, 중복 인증 불가 
  - 매주 일요일 0시, 인증 상태 초기화 (스케줄링)

### 5️⃣ 리뷰/댓글
- 리뷰(일반) : 특정 영화에 대해 한 번만 작성 가능
- 댓글 : 특정 리뷰에 대해 여러 개 작성 가능
### 6️⃣ 좋아요
- 특정 리뷰에 대해 한 번만 가능
  - 두 번 누를 시 취소
- 좋아요 순 리뷰 정렬 기능 제공
### 7️⃣ 토론 (인증 리뷰)
- 인증 승인을 받은 사용자만 작성할 수 있는 토론 게시판 제공 
- 인증하기 전에 작성했던 리뷰가 있는 경우
  - 이전의 리뷰는 삭제되고, 인증받은 후의 리뷰만 저장됨
- 리뷰 조회시, 인증된 리뷰만 필터링하여 조회 가능
- 토론 게시판에서 작성된 리뷰에는 '인증된 리뷰' 표시 부착
### 8️⃣ 알림 (SSE 기반)
- 발생 조건  
  - 리뷰에 댓글이 달릴 때
  - 리뷰에 좋아요가 눌릴 때
  - 인증 상태가 변경될 때
- 본인의 행동에 대해서는 알림이 가지 않음
  - 타인에 의해서만 알림 작동
