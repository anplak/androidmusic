# Linter configuration

All static-analysis config for this repo lives here:

| File | Tool | Purpose |
|------|------|---------|
| `detekt.yml` | Detekt | Rule config; complexity thresholds match current UI/DAO ceilings |
| `detekt-baseline.xml` | Detekt | Optional suppressed smells (prefer raising thresholds over growing this) |
| `lint-baseline.xml` | Android Lint | Deferred findings only (currently `OldTargetApi`) |

Gradle wiring is in [`app/build.gradle.kts`](../../app/build.gradle.kts):

- Lint baseline: `rootProject.file("config/linters/lint-baseline.xml")`
- Detekt config/baseline: `config/linters/detekt.yml` / `detekt-baseline.xml`

Dependency version warnings (`GradleDependency`, `NewerVersionAvailable`, `AndroidGradlePluginVersion`) and `ObsoleteSdkInt` (launcher adaptive icons in `mipmap-anydpi-v26`) are **disabled** in Lint.

Refresh Lint baseline after intentional fixes:

```bash
./gradlew :app:updateLintBaseline
```

Refresh Detekt baseline only when deliberately deferring remaining smells:

```bash
./gradlew :app:detektBaseline
```
