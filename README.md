# 모아 ✦

보고 싶은 공연, 여기 다 모아. 전국 공연 탐색 Android 앱.

## 실행

Android Studio에서 이 폴더를 열고 Gradle 동기화 후 `app`을 실행합니다. JDK 17 이상, Android SDK 35, Android 10 이상 기기가 필요합니다.

```powershell
./gradlew.bat :app:assembleDebug
./gradlew.bat :app:assemblePreview
./gradlew.bat :app:assembleRelease :app:bundleRelease
./gradlew.bat :app:testDebugUnitTest :app:lintDebug
./gradlew.bat :app:connectedDebugAndroidTest
python -m unittest discover -s tools/tests -v
```

Windows에서 한글 경로로 인해 Gradle 테스트가 `ClassNotFoundException`으로 실패한다면 `tools/build-windows.ps1`을 실행하세요. ASCII 경로의 전용 빌드 폴더에서 검증하고 APK를 `dist`에 복사합니다. 에뮬레이터 자동 테스트까지 포함하려면 `tools/build-windows.ps1 -VerifyDevice`를 실행합니다.

출시 준비 산출물까지 다시 생성하려면 `tools/build-windows.ps1 -ReleaseArtifacts`를 실행합니다. 자세한 실제 데이터 연결 절차는 [docs/data_connection.md](docs/data_connection.md)에 있습니다.

## 포함 기능

- 라벤더·핑크 브랜드 테마, 모아 로고 및 아이콘
- 발견 / 검색 / 찜 / 설정 / 공연 상세
- 전국 시·도 선택, 복수 장르 선택, 날짜 기간 겹침 필터, 직접 날짜 선택
- 공연명·출연진 검색, 검색 기록 삭제, 시작일·종료일 정렬
- Room 기반 오프라인 목록 캐시와 찜, DataStore 선호 설정
- 공식 예매처 선택 및 외부 브라우저 연결, 예매 URL 누락 안내
- HTTPS JSON 연결, 갱신 실패 시 이전 데이터 유지
- 목록과 갱신 정보의 트랜잭션 저장, 다운로드 중 배포 버전 변경 감지
- KOPIS Python 수집·정규화·월별 중복 인덱싱, GitHub Pages 예약 배포 워크플로

첫 실행은 디자인 체험용 **가상 공연 12개**를 제공합니다. 실제 공연·가격·예매 정보가 아니며 예매 버튼을 비활성화합니다. 사용자 API 키를 앱에 저장하지 않습니다.

## 실제 공연 데이터 연결

1. [KOPIS OpenAPI](https://www.kopis.or.kr/por/cs/openapi/openApiInfo.do?menuId=MNU_00074) 서비스키를 발급하고 JSON 재배포·포스터 사용 조건을 확인합니다.
2. GitHub 저장소 Secrets에 `KOPIS_API_KEY`를 등록합니다.
3. GitHub Settings → Pages → Source를 **GitHub Actions**로 설정합니다.
4. `.github/workflows/data.yml`을 저장소에 올린 뒤 `Collect and publish performances`를 수동 실행합니다.
5. 앱 설정에서 `https://sung-gyeonghun.github.io/MOA/`를 저장하고 데이터 갱신을 누릅니다. 실제 배포 URL은 Actions 결과로 확인하세요.

예약 수집은 UTC 기준 3시간마다 17분에 실행합니다. GitHub 실행이 지연될 수 있습니다. 현재 수집은 6개월의 예정·진행 공연 전체 동기화입니다. `afterdate` 증분 수집은 실제 API 검증 전까지 활성화하지 않습니다. 한 요청이라도 실패하거나 빈 데이터이면 배포하지 않아 이전 Pages 배포가 유지됩니다.

## 구조

`app/src/main/java/kr/moa/app/data` : 정규화 모델, 필터, Retrofit·Room·DataStore 저장소

`MoaViewModel.kt` : 상태 및 사용자 동작 · `MainActivity.kt` : Compose 화면 / Navigation

`MoaApplication.kt` : Hilt 의존성 주입 · `tools/collect.py` : 서버 없는 데이터 파이프라인

KOPIS 필드 매핑 기준은 [공식 개발 가이드](https://kopis.or.kr/upload/openApi/공연예술통합전산망OpenAPI개발가이드.pdf)입니다.

## 배포 전 남은 검증

- 실제 서비스키로 데이터 커버리지·예매 링크·HTTP 포스터의 HTTPS 대응 확인
- 데이터 이용 및 재배포 조건 확인, 실제 전국 데이터 크기와 요청량 측정
- 실제 Android 기기 검증, 배포용 서명키 준비 및 Play 정책 검토

`dist/moa-preview.apk`는 용량을 줄인 설치용 APK이고 `dist/moa-debug.apk`는 디버깅용 APK입니다. 둘 다 개발용 서명입니다. Play 출시용으로 사용하지 마세요. 외부 서비스 설정과 실제 API 검증을 마치기 전에는 운영 배포가 완료된 상태가 아닙니다.

`dist/moa-release-unsigned.apk`와 `dist/moa-release-unsigned.aab`는 출시 준비용 미서명 산출물입니다. 설치·스토어 업로드 전 소유자의 정식 서명키 설정이 필요합니다. 저장소에 서명키를 포함하지 않았습니다.

## 화면 미리보기

`docs/screenshots`에 에뮬레이터에서 캡처한 홈·검색·상세·찜·설정·필터 화면이 있습니다.

HTTPS 모의 서버와 실제 Room/DataStore를 이용해 오류 응답·잘못된 JSON·빈 데이터·버전 변경 시 캐시 유지, 오프라인 재조회, 설정과 찜 재복원, 기존 DB 마이그레이션을 검증합니다. Android 테스트는 에뮬레이터 또는 연결된 개발용 기기가 필요합니다.
