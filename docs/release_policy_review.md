# 공개 출시 검토 기록

검토일: 2026-10-13. 현재 Preview는 시험 설치용이며 Play 제출은 진행하지 않았다.

## KOPIS 데이터

[공식 OpenAPI 안내](https://kopis.or.kr/por/cs/openapi/openApiInfo.do?menuId=MNU_00077)는 인증키와 KOPIS 기반 서비스의 출처 표시를 요구한다. 앱 상세·설정과 공개 데이터 페이지에 출처를 표시했다. 사용자는 2026-10-13 발급 약관 또는 담당자 안내에서 JSON 재배포와 제공 포스터 표시가 허용됨을 확인했다고 답했다. 이 기록은 사용자 확인이며 독립적인 법률 검증이나 권리 양도 문서는 아니다.

## 개인정보 및 Google Play

[Google Play User Data 정책](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en)은 데이터 처리 공개와 개인정보처리방침을 요구한다. [Data safety 안내](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en-GB)에 따라 실제 동작과 포함 SDK를 기준으로 제출 내용을 작성해야 한다.

현재 소스 검토 결과:

- 앱 매니페스트 권한은 INTERNET 하나다. 위치·연락처·카메라·광고 ID 권한은 없다.
- 계정·분석·광고 SDK 없이 Room에 공연과 찜, DataStore에 검색 기록과 선택 지역·장르·데이터 주소를 저장한다.
- 지역은 사용자가 선택하며 GPS를 읽지 않는다. 기기 백업은 비활성화했다.
- 검색은 캐시에서 처리한다. 데이터 및 포스터 HTTPS 요청은 GitHub Pages 또는 원본 이미지 호스트에 전송된다. 해당 호스트는 통상적인 접속 메타데이터를 처리할 수 있다.
- 공식 예매 링크는 외부 브라우저를 연다. 예매처의 개인정보 처리는 해당 서비스 정책을 따른다.

운영자 성경훈과 skh1205@naver.com을 포함한 공개 개인정보처리방침 및 설정 내 열람을 v1.0.3에 구현했다. 실기기 주요 기능 성공은 사용자가 확인했다. 실제 Play 제출 시 남는 작업은 Play Console Data safety 입력과 소유자 릴리스 서명이다. 현재 검토는 정책 준수 인증이나 Play 심사 통과를 의미하지 않는다.

## 실제 휴대폰 확인 절차

`dist/moa-preview.apk`를 Android 10 이상 휴대폰에 설치한 뒤 전국 목록 → 대구만 표시 → 공연명·출연진 검색 → 상세·예매처 브라우저 이동 → 찜 후 강제 종료·재실행 → 비행기 모드 캐시 조회 → 글꼴 확대·TalkBack 읽기 순서로 확인한다. 개인정보 입력이나 실제 결제는 필요하지 않다.

ADB 연결이 제공되면 동일 절차와 연결 테스트를 실행할 수 있다. 현재 ADB 연결 장치는 에뮬레이터뿐이지만 사용자가 실제 휴대폰 주요 기능의 정상 동작을 확인했다. `verification/physical-device-user-confirmation.md` 참고.
