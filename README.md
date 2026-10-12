# 모아 ✦

보고 싶은 공연, 여기 다 모아. 전국 공연 탐색 Android 앱.

## 설치와 사용

`dist/moa-preview.apk`를 Android 10 이상 기기에 설치합니다. 개발용 서명이며 Play 출시용 서명은 아닙니다.

- **실제 국내 공연 2,934건**을 초기 데이터로 포함합니다. 대구 공연 171건.
- 앱 실행 시 [공연 데이터 서버](https://sung-gyeonghun.github.io/MOA/)에서 자동 갱신합니다. 앱에 API 키를 입력할 필요가 없습니다.
- 지역·복수 장르·오늘/주/주말/월/직접 날짜 필터, 공연명·출연진 검색, 상세·공식 예매처 이동, 찜을 제공합니다.
- 마지막 정상 목록과 찜은 Room에, 선호와 검색 기록은 DataStore에 저장됩니다.
- 오프라인에서도 초기 데이터 또는 마지막 정상 캐시를 볼 수 있습니다.
- Pretendard Variable, 제공된 모아 로고·아이콘, 라벤더·핑크 테마를 적용했습니다.

**Debug 빌드**는 개발·UI 테스트용 가상 공연을 기본으로 사용합니다. 실제 데이터로 사용하는 설치 파일은 **Preview APK**입니다. 이전 체험용 찜은 체험용으로 표시됩니다.

## 데이터 운영

KOPIS → Python / GitHub Actions → GitHub Pages JSON → Android.

- 저장소 Actions Secret `KOPIS_API_KEY` 등록 완료. 키는 소스·JSON·APK에 포함하지 않습니다.
- GitHub Pages의 workflow 배포 설정 완료.
- `17 */3 * * *` UTC 예약 실행과 수동 실행 제공. 예약 시각은 지연될 수 있습니다.
- 변경일 필터를 실제 API로 확인했습니다. 정상 배포 캐시를 재사용하고 변경된 상세·새 공연을 갱신합니다. 7일마다 전체 상세 동기화합니다.
- KOPIS 상세 API의 일시적 HTTP 400을 관찰해 재시도와 중간 저장을 보강했습니다.
- 수집·검증 실패 시 배포하지 않습니다. 앱은 파일 SHA256·버전을 확인한 후 목록과 갱신 정보를 한 트랜잭션으로 저장합니다.
- 현재 API 지역 표기 `전남광주통합특별시`는 앱에서 `전남광주`로 표시합니다. 원본의 해외 4건은 국내 범위에서 제외했습니다.

[배포 확인](https://github.com/sung-gyeonghun/MOA/actions/runs/38279518647) · [실제 증분 갱신 성공](https://github.com/sung-gyeonghun/MOA/actions/runs/38280553834)

증분 검증 실행은 상세 168건을 갱신하고 2,766건을 재사용했습니다. 실제 예약 이벤트가 실행된 이력은 아직 관찰하지 않았습니다.

## 개발·검증

Kotlin / Compose Material 3 / MVVM / Coroutines + Flow / Hilt / Navigation / Retrofit + OkHttp / Kotlinx Serialization / Room / DataStore / Coil.

Windows 한글 경로에서 Gradle 테스트 클래스 로딩 오류가 발생하면 전용 ASCII 빌드 경로를 사용하는 아래 도우미를 실행합니다.

```powershell
./tools/build-windows.ps1 -VerifyDevice -ReleaseArtifacts
python -m unittest discover -s tools/tests -v
python tools/validate_snapshot.py public
```

Android Studio에서도 프로젝트를 열 수 있습니다. JDK 17 이상과 Android SDK 35가 필요합니다.

검증: Kotlin 테스트 5개, Python 테스트 16개, Android 에뮬레이터 테스트 12개 통과. 실제 공연 상세·찜·프로세스 종료 후 유지·오프라인 목록과 공개 JSON 파일의 해시를 추가 확인했습니다. 실제 Android 기기와 Android 10 기기 시험은 아직 하지 않았습니다.

`docs/verification`에 테스트 XML·배포·실제 데이터·기기 확인 및 파일 SHA256 기록이 있습니다. `docs/screenshots`에 실제 공연 데이터로 캡처한 화면이 있습니다.

## 산출물과 출시 전 단계

- `dist/moa-preview.apk`: 실제 공연 데이터 포함, 최적화된 개발용 설치 APK
- `dist/moa-debug.apk`: 개발·테스트 APK
- `dist/moa-release-unsigned.apk`, `dist/moa-release-unsigned.aab`: 미서명 출시 준비 산출물

일반 공개/Play 출시 전 소유자의 서명키, 실기기 시험, 개인정보 및 콘텐츠 이용 권한 검토가 필요합니다. 앱의 데이터 출처 표시는 [KOPIS OpenAPI 안내](https://kopis.or.kr/por/cs/openapi/openApiInfo.do?menuId=MNU_00077)를 반영했고, [공통코드](https://www.kopis.or.kr/upload/openApi/공연예술통합전산망OpenAPI공통코드.pdf)의 장르 코드를 사용합니다.

Pretendard는 SIL Open Font License 1.1로 배포되며 라이선스를 APK의 `assets/licenses/pretendard.txt`에 포함했습니다.
