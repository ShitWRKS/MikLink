# S5 Baseline

Date: 2025-01-27  
Epic: S5 — Decomposizione AppRepository + UseCase "Run Test"

## Baseline Build Results

- `./gradlew :app:kspDebugKotlin` -> **PASS**
- `./gradlew assembleDebug` -> **PASS**
- `./gradlew testDebugUnitTest` -> **PASS**

## Current State

- TestViewModel orchestrates test execution directly (lines 74-675)
- AppRepository_legacy contains all test operations (runCableTest, getLinkStatus, runPing, runSpeedTest, etc.)
- No separation between orchestration (ViewModel) and domain logic
- No UseCase layer for test execution

## Migration Target

- Extract test orchestration to RunTestUseCase
- Create domain contracts for test steps
- Create repository interfaces in core/data
- Implement repositories using Room v1 + MikroTik REST
- Migrate TestViewModel to use UseCase instead of direct repository calls

