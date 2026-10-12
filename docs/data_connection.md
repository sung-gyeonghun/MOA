# 실제 공연 데이터 연결

현재 앱은 가상 공연으로 디자인과 동작을 체험할 수 있습니다. 실제 데이터 연결 전 아래 외부 설정이 필요합니다.

## 1. KOPIS 서비스키

[KOPIS OpenAPI 안내](https://www.kopis.or.kr/por/cs/openapi/openApiInfo.do?menuId=MNU_00074)에서 서비스키를 신청합니다. 신청서 활용 목적에 사용할 수 있는 초안:

> 모아는 전국 공연을 지역·장르·날짜 및 공연명·출연진으로 탐색하는 Android 앱입니다. KOPIS의 공연 목록·상세 정보를 이용해 공연 정보와 출처를 표시하고, 제공된 공식 예매처 URL로 연결합니다. API 키는 앱에 포함하지 않고 서버 측 예약 작업의 비밀 값으로 관리할 계획입니다. 개인 사용으로 시작하며 추후 공개 여부를 검토합니다.

앱 구조는 수집한 정보를 GitHub Pages JSON으로 제공하도록 준비했습니다. **JSON 재배포 및 포스터 표시가 허용되는지는 별도 확인**해야 합니다. 이용 조건이 해당 구조를 허용하지 않으면 배포 구조를 조정해야 합니다.

## 2. GitHub 설정

- 저장소: `sung-gyeonghun/MOA` (현재 Git 원격 주소 기준)
- [Actions Secrets 설정](https://github.com/sung-gyeonghun/MOA/settings/secrets/actions): `KOPIS_API_KEY`라는 이름으로 키 등록
- [Pages 설정](https://github.com/sung-gyeonghun/MOA/settings/pages): Build and deployment의 Source를 `GitHub Actions`로 선택

키는 채팅·Kotlin·Python 소스·JSON·일반 설정 파일에 붙여넣지 않습니다. 앱에는 키 입력 기능이 없습니다.

## 3. 첫 수집 확인

코드가 원격 저장소에 올라간 후 Actions의 `Collect and publish performances`에서 Run workflow를 실행합니다. 테스트 → 수집 → 검증 → 업로드 → Pages 배포 순서입니다. 수집이나 검증이 실패하면 배포 단계로 진행하지 않습니다.

배포 결과의 URL에서 `manifest.json`을 확인합니다. 예상 주소는 `https://sung-gyeonghun.github.io/MOA/`이지만 실제 주소는 Actions 배포 결과가 기준입니다.

## 4. 앱 연결

앱 설정 → GitHub Pages 데이터 주소 입력 → 주소 저장 → 데이터 갱신.

정상 수집된 목록은 Room에 캐시됩니다. 갱신 시각과 목록은 함께 저장됩니다. 갱신 실패·파싱 오류·빈 데이터·다운로드 중 버전 변경이면 이전 정상 캐시가 유지됩니다.

## 5. 실제 데이터로 확인할 항목

- 대구 선택 시 실제 대구 공연만 표시되는지
- 콘서트·뮤지컬·연극 등의 원본 장르 및 코드가 올바르게 매핑되는지
- 공연 기간이 월을 넘어가도 검색/날짜 필터에서 빠지지 않는지
- 출연진·가격·관람 연령·시간·포스터·예매 링크의 제공 범위
- 파일 크기, API 요청량과 수집 시간, 무료 운영 한도
- 비행기 모드에서 마지막 목록 조회 및 찜 유지

현재 `afterdate` 증분 수집은 실제 API 검증 전이라 활성화하지 않았습니다. 최초 연결은 6개월 전체 동기화이며, 수집 범위·호출량을 확인한 뒤 증분 수집과 정기 전체 동기화를 확정합니다.
