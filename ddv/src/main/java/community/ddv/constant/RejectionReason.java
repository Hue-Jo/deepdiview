package community.ddv.constant;

public enum RejectionReason {

  NONE,
  OTHER_MOVIE_IMAGE,   // 다른 영화 사진 업로드
  WRONG_IMAGE,         // 관계없는 사진 업로드
  UNIDENTIFIABLE_IMAGE // 식별불가 사진 업로드
}
