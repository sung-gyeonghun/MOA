# 모아 구현 현황 — 2026-10-13

## 실제 연결 완료

- 제공된 KOPIS 키 인증·목록·상세·출연진·가격·공식 예매 URL 확인
- GitHub Actions Secret 암호화 등록, 공개 소스에 키 없음
- 국내 2,934건, 대구 171건, 장르 코드 9개(표시 장르 8개), 월 경계 공연 388건 확인
- 해외 4건 제외, 현재 API의 전남광주 지역 표기 반영
- Pages 공개 배포와 월별 SHA256 검증 성공
- 실제 증분 갱신 성공: 상세 168건 갱신 / 2,766건 재사용
- 예약 하루 1회(한국 시간 09:17) + 수동 실행 + 주간 전체 동기화 구성; 실제 예약 이벤트 수집·배포 성공 확인 (38328864571)

## 앱

제공 로고·아이콘, Pretendard, 라벤더·핑크 Compose 화면과 발견/검색/상세/찜/설정, 필터·정렬·공식 예매처 이동 구현. 실제 초기 데이터를 포함하고 Pages 자동 갱신. 오프라인 Room 캐시 및 찜, DataStore 선호·검색 기록. 갱신 실패/빈 데이터/파싱/해시/버전 변경에 기존 캐시 보존. 기존 DB 마이그레이션과 체험용 저장 항목 표시 유지.

## 검증과 산출물

- 최신 Android 10 연결 테스트 15개 / Kotlin 8개 / Python 16개 통과
- Android 10(API 29)에서 최신 Android 테스트 15개 통과. Preview APK 설치와 실제 2,934건 표시, 오프라인 재실행 확인.
- 실제 공연 상세·찜·프로세스 종료 후 찜 유지·오프라인 목록 확인
- Preview APK 설치, 공개 JSON 전 파일의 SHA256 확인
- Debug / Preview / 미서명 Release APK 및 AAB 생성
- 정적 검사 오류 0개, 최신 의존성/SDK 관련 경고 존재
- 합성 5,000건 캐시 로드 2초 이내 확인; 실제 데이터로도 목록 표시 확인

근거: `docs/verification`, `docs/screenshots`, [부트스트랩 배포](https://github.com/sung-gyeonghun/MOA/actions/runs/38279518647), [증분 갱신](https://github.com/sung-gyeonghun/MOA/actions/runs/38280553834).

## 아직 완료되지 않은 출시 검증

소유자 정식 서명키, 실제 Play 제출 시 Data safety 입력과 소유자 최종 정책 확인. Android 10은 에뮬레이터로 검증했으며 Play 출시를 완료한 상태는 아닙니다.

최신 UI: 배너 지역 알약 버튼, 지역·장르 복수 선택 드롭다운, 월 단위 기간 선택, 설정 주소 제거. 변경 검증은 Kotlin 8개·Android 10 연결 13개 통과. `docs/verification/dropdown-month-update.md` 참고.

설치용 Preview APK는 GitHub Release `v1.0.3-preview.1`로 배포하며 README에서 직접 다운로드합니다.

실기기 검증: 사용자가 2026-10-13 검색·상세·예매 링크 열기·찜 후 종료/재실행·비행기 모드 조회 모두 정상이라고 확인했다. 기종·OS·측정 성능은 별도로 수집하지 않았다. 다크모드 저장과 키보드 하단 메뉴 처리는 `appearance-keyboard-tests.xml`에서 확인했다.

최종 추가: 개인정보처리방침을 운영자 성경훈(skh1205@naver.com) 정보와 함께 공개하고 설정에서 오프라인 열람하도록 구현. 데이터 JSON 재배포·제공 포스터 표시 허용은 사용자 확인으로 기록했다. 최신 Debug/Preview/Release APK·AAB 모두 1.0.3으로 빌드했다.
