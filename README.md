# 영화에 대한 심도 있는 토론을 원한다면? Deepdiview! 
## 
TMDB의 오픈API를 활용하여
넷플릭스(kr)에서 제공하는 영화정보를 제공하고 
자신이 본 영화에 대한 다양한 의견을 나눌 수 있는 서비스입니다. 

-----
## 사용 기술 스택
- 언어 : Java (JDK 17)
- 프레임워크 : SpringBoot 3.4
- DB : MySQL, Redis
- 파일 저장 : AWS S3
- ORM : Spring Data JPA
- 보안 : Spring Security, JWT (jjwt 0.12)
- 실시간 통신 : SSE
- 배포 환경 : AWS EC2, Github Actions (CI/CD)
- API 문서화 : Swagger


### 사용 오픈 API
- TMDB https://api.themoviedb.org/3/discover/movie?include_adult=true&include_video=false&language=ko&sort_by=primary_release_date.desc&watch_region=KR&with_watch_providers=8

---

## 프로젝트 기능 및 설계 
### 1. 사용자
- 사용자는 일반 사용자(USER), 관리자(ADMIN)로 나뉩니다.
- JWT를 이용하여 로그인 후 인증된 사용자만 특정 기능을 사용할 수 있습니다.
- 로그인 시, 엑세스 토큰과 리프레시 토큰이 생성됩니다.
### 2. 영화 조회
- TMDB API로 영화/장르정보를 받아옵니다.
  - 월요일 0시마다 스케줄링
- 넷플릭스 코리아의 인기도 탑20의 영화리스트 조회 
- 영화 제목으로 영화 상세정보 조회
  - 특정 단어가 포함되어 있는 영화의 상세정보를 리스트로 반환
  - 띄어쓰기를 무시하고도 조회 가능
### 3. 리뷰/댓글
- 리뷰 : 특정 영화에 대해 한 번만 작성 가능
- 댓글 : 특정 리뷰에 대해 여러 댓글 작성 가능
### 4. 좋아요
- 특정 리뷰에 대해 한 번만 가능
  - 두 번 누를 시 취소
### 5. 투표
- 생성
  - 관리자만 가능 
  - 일요일에만 가능
  - 인기도 탑 5개의 영화가 선택지로 생성됨
- 참여
  - 월요일부터 토요일까지 가능
  - 한 주에는 한 번만 참여 가능
  - 하나의 영화에만 투표할 수 있음
  - 투표 후 결과를 바로 확인 가능
- 동일 득표수로 1위를 가릴 수 없다면 마지막에 득표한 영화가 상위 랭크 차지 
### 6. 인증
- 인증 대상 영화 : 지난주의 투표에서 1위를 한 영화
- 인증 방법 : 해당 영화를 보는/본 사진 업로드
- 인증 상태 : 보류, 승인, 거절 (일주일마다 초기화 됨)
  - 이미 승인을 받은 경우, 중복 인증 불가  
- 인증 처리 : 관리자 - 업로드된 사진을 확인한 후 인증 승인/거절(사유포함)

### 7. 토론
- 토론 게시판 = 인증 승인을 받은 사용자(영화를 본 사용자)만 리뷰/댓글을 작성할 수 있는 게시판 
- 인증하기 전에 작성했던 리뷰가 있는 경우
  - 이전의 리뷰는 삭제되고, 인증받은 후의 리뷰만 저장됨
- 인증받은 사용자가 작성한 리뷰의 경우, '리뷰에 인증된 리뷰' 표시가 붙음 
### 8. 알림
- 알림 종류 
  - 리뷰에 댓글이 달릴 시
  - 리뷰에 좋아요가 눌릴 시
  - 인증 상태가 변경될 시
- 알림 조건
  - 타인에 의했을 때만 알림이 감  
  - 자신이 자신의 리뷰에 댓글을 달거나 좋아요를 누를 때는 알림이 가지 않음 
