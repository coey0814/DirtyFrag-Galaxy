# DirtyFrag-Galaxy v2.0

DirtyFrag(CVE-2026-43284) 기반 삼성 갤럭시용 원클릭 임시 루트.

## 2.0으로 바뀐 이유

- **Fold8 울트라 전용 → 갤럭시 범용**으로 전환. 앱 ID를 `dirtyfrag.galaxy` 로 변경하고,
  커널별 KO 자동 선택과 기기 무관 판정을 적용했습니다.
- **Shizuku/PC/ADB 불필요** — 앱 단독 원클릭으로 익스플로잇부터 LSPosed 주입 확인까지 수행합니다.
- **permissive 선택화 + 무재부팅 Enforcing 복원** — permissive가 필요 없는 기기는 그대로 동작하고,
  필요한 기기는 루트·Zygisk·LSPosed 확인 후 재부팅 없이 SELinux를 Enforcing으로 되돌립니다.
- **자동 소프트 재시작 기본 OFF** — 불필요한 soft reboot 루프를 제거했습니다.
- **LSPosed 주입 판정 정확화** — 현재 `system_server` pid 기준으로 오탐/부정 제거.
- **미취약 커널 안전 처리** — CBC 프리미티브 no-op(예: 6.1)을 검증 단계에서 감지하고 명확히 중단.

## 검증

| 기기 | 커널 | 결과 |
|---|---|---|
| SM-F976N (Fold8 Ultra) | 6.12.58-android16 | ✅ |
| SM-S931N (S25) | 6.6.98-android15 | ✅ |
| SM-S926N (S24+) | 6.1.157-android14 | ❌ 미취약 |

## 요구사항

- KernelSU 매니저 앱(별도 설치)
- (모듈 사용 시 권장) Zygisk-Next, LSPosed

## 설치

1. KernelSU 매니저,LSPosed 매니저 설치
2. 이 릴리즈의 APK 설치
3. 앱 실행 → 루팅 → 매니저에서 ROOT 허용 → 
4. (LSPosed 사용 시) Zygisk-Next 및 LSPosed 설치 후 소프트 재시작

서명: AOSP testkey (재현 가능 빌드).
