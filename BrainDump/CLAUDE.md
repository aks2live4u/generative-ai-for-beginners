# Notes for future Android/Kotlin work in this repo

## Kotlin ktx extension functions need explicit imports

Kotlin extension functions from AndroidX ktx artifacts (e.g.
`androidx.activity.addCallback`, `androidx.fragment.app.viewModels`,
`androidx.lifecycle.lifecycleScope.launch` helpers, etc.) are **not**
auto-resolved just because the artifact is a dependency — they need an
explicit `import` matching the function's package, otherwise Kotlin may
silently resolve the call to a different overload (e.g. the raw Java
member method) and produce confusing errors like:

```
Type mismatch: inferred type is () -> Unit but OnBackPressedCallback was expected
Unresolved reference: isEnabled
```

This happened with `onBackPressedDispatcher.addCallback(this) { isEnabled = ... }`
in `MainActivity.kt` — fixed by adding `import androidx.activity.addCallback`.

**When writing any Kotlin code that uses a "lambda with receiver" style API
from an AndroidX ktx package, double-check the matching top-level import is
present** — don't rely on it being implicit. Run `./gradlew compileDebugKotlin`
(or equivalent) before considering Kotlin changes done, since these errors
only surface at compile time, not in a quick read-through.
