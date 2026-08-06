# Clone / design-system fidelity review: design_system_integrity

## Verdict

**Recommendation: REQUEST_CHANGES.** This is a promising palette and typography foundation, rendered with real Compose primitives, but it is not wired into either platform's live app. It also contains failing color-pair contracts and leaves most Material 3 roles at the unrelated baseline scheme. It therefore cannot currently be considered a well-made, reusable Compose Multiplatform design system.

## Scope and limitations

- Goal reviewed: whether the newly added Compose Multiplatform design system is well made.
- Target/reference design: none supplied.
- Screenshots or runtime capture: none supplied.
- Visual-fidelity conclusion: **not assessable** without a target and a rendered result. No approval claim is made for visual similarity.
- Working-tree note: the three theme files are untracked, so `git diff` contains no textual diff for them. I inspected their current on-disk contents directly. `App.kt` is unchanged in the available diff but is essential to verifying integration.

## Evidence inspected

1. `shared/src/commonMain/kotlin/com/yeobaek/core/designsystem/theme/Color.kt` (current on-disk source, lines 1-26)
2. `shared/src/commonMain/kotlin/com/yeobaek/core/designsystem/theme/Theme.kt` (current on-disk source, lines 1-57)
3. `shared/src/commonMain/kotlin/com/yeobaek/core/designsystem/theme/Type.kt` (current on-disk source, lines 1-102)
4. `shared/src/commonMain/kotlin/com/yeobaek/App.kt` (current on-disk source, lines 1-49)
5. `androidApp/src/main/kotlin/com/yeobaek/MainActivity.kt` (lines 10-18) and `shared/src/iosMain/kotlin/com/yeobaek/MainViewController.kt` (line 5), to verify both platform entries.
6. `gradle/libs.versions.toml` and the cached `org.jetbrains.compose.material3:material3:1.11.0-alpha07` `ColorScheme.kt`, to verify the Material 3 role surface used by this project.
7. `./gradlew :shared:compileCommonMainKotlinMetadata --no-daemon` — passed. An initial request for a non-existent `:shared:compileKotlinAndroid` task failed only because the task name does not exist; it is not a source-compilation failure.
8. Provided contrast measurements: `#FFFFFF/#B59A7D` 2.66:1; `#8C857D/#FFFFFF` 3.64:1; `#8C857D/#F5F0E8` 3.21:1; `#3C3833/#FAF7F2` 10.88:1; `#6B645E/#FAF7F2` 5.44:1.

## Findings

### CRITICAL

None found. The inspected UI is live Compose (`Column`, `Button`, `Text`, and `AnimatedVisibility`), not a pasted screen or CSS/background-image substitute.

### HIGH

1. **The application never applies `YeobaekTheme`, so the design-system values are dead code in both shipped platform paths.**
   - Evidence: `shared/src/commonMain/kotlin/com/yeobaek/App.kt:25` installs a fresh `MaterialTheme { ... }` rather than `YeobaekTheme { ... }`. `YeobaekTheme` is defined only at `shared/src/commonMain/kotlin/com/yeobaek/core/designsystem/theme/Theme.kt:47-57`, and repository search finds no call site. Android reaches `App()` at `androidApp/src/main/kotlin/com/yeobaek/MainActivity.kt:15-17`; iOS reaches it at `shared/src/iosMain/kotlin/com/yeobaek/MainViewController.kt:5`.
   - Impact: the screen uses Material defaults rather than the stated palette and typography. There is no actual theme reuse or cross-platform design-system consistency to evaluate at runtime.
   - Required change: make the common root install `YeobaekTheme` (or have each platform root do so exactly once) and add a smoke/UI test or preview proving `MaterialTheme.colorScheme` and `MaterialTheme.typography` resolve to these tokens.

2. **Several declared content/background contracts fail normal-text accessibility, including a standard Material 3 `on*` role.**
   - Evidence: `onSecondary = Color.White` over `secondary = YeobaekAccent` at `shared/src/commonMain/kotlin/com/yeobaek/core/designsystem/theme/Theme.kt:16-17` measures 2.66:1. This fails the 4.5:1 normal-text threshold and the 3:1 large-text threshold. `onSurfaceVariant = YeobaekTextMuted` at `Theme.kt:34-35` measures 3.21:1 against its paired `surfaceVariant` (`YeobaekSoft`); it also measures only 3.64:1 against white surfaces. `bodySmall` is only 11sp at `Type.kt:75-81`, so these colors cannot be excused as large display text.
   - Impact: buttons, selection controls, and component secondary text may be unreadable. In particular, Material components can consume `onSecondary` and `onSurfaceVariant` automatically, making this a system-level defect rather than a single-screen exception.
   - Required change: choose compliant `onSecondary` and `onSurfaceVariant` token pairs, then test every declared `foreground/background` contract used by components at its intended text size.

### MEDIUM

1. **The Material 3 color scheme is incomplete: 22 of 48 current roles are provided; the remainder silently come from Material's default purple/neutral baseline.**
   - Evidence: `shared/src/commonMain/kotlin/com/yeobaek/core/designsystem/theme/Theme.kt:8-44` supplies primary/secondary/tertiary core roles, background/surface, outline, and two error roles. The installed Material 3 `ColorScheme` also has inverse, error-container, scrim, surface-container, and primary/secondary/tertiary fixed role families. They are omitted here, except that `surfaceTint` happens to default to the supplied `primary`.
   - Impact: snackbars, elevated/container surfaces, fixed-color components, and error containers can introduce colors outside the Yeobaek system. The design will drift when more standard Material components are introduced.
   - Required change: explicitly tokenise and map all roles that the supported component set can use, especially inverse, error-container, scrim, surface-container, and fixed roles; verify them in component previews.

2. **This is a theme foundation, not yet a rigorous reusable design system.**
   - Evidence: `Color.kt:6-26` contains color constants, `Type.kt:12-102` contains typography, and `Theme.kt:8-57` maps them to Material. There are no brand shape, spacing, elevation, state, semantic-status, or reusable component primitives. Material's default shapes are used because `MaterialTheme` at `Theme.kt:52-56` receives no `shapes` argument. The app itself has no Yeobaek component usage.
   - Impact: spacing and component appearance will become one-off values as screens grow, and there is no shared abstraction enforcing the intended layer hierarchy.
   - Required change: define the intended token tiers (foundation -> semantic -> component), provide brand `Shapes` and a spacing/elevation strategy, and introduce shared component wrappers for the recurring controls before claiming a design system.

3. **The typography system is partial and will fall back to default Material styles; generic font families also do not guarantee a consistent brand on Android and iOS.**
   - Evidence: `Type.kt:9-10` uses `FontFamily.Serif` and `FontFamily.SansSerif`, which resolve to platform fonts. `Typography` at `Type.kt:12-102` customizes nine styles but omits `displayLarge`, `displayMedium`, `displaySmall`, `headlineSmall`, `titleSmall`, and `labelSmall`, leaving their default Material definitions.
   - Impact: visual type hierarchy is inconsistent across unfilled slots and may vary materially by platform, particularly for Korean glyph rendering.
   - Required change: decide whether platform-native typography is intentional. If not, package the selected cross-platform font family (with Korean coverage) and define every text style that components/screens are permitted to use.

4. **Only a light scheme exists, with no documented system-dark-mode policy.**
   - Evidence: `Theme.kt:8` defines only `YeobaekLightColorScheme`; `YeobaekTheme` at `Theme.kt:47-56` always selects it.
   - Impact: the product will either remain light-only without communicating that choice or will lack a maintainable dark-mode path when system/theme support is added.
   - Required change: explicitly declare a light-only product policy and test it, or add a dark scheme selected through a `darkTheme` parameter/system setting.

### LOW

1. **The raw-value discipline is good for the current small scope, but the naming does not yet separate foundation tokens from semantic aliases.**
   - Evidence: raw colors are centralized in `Color.kt:6-26`, and the theme mostly consumes those values in `Theme.kt:8-44`; this avoids hardcoded colors in screen code. However, `YeobaekInk` and `YeobaekTextPrimary` duplicate the same value at `Color.kt:7` and `Color.kt:16`, while `Theme.kt:11`, `:17`, and `:23` bypass the local token file with `Color.White`.
   - Impact: future palette changes can diverge across aliases; it is not currently a runtime defect.
   - Required change: retain raw palette values in foundation tokens, create semantic `on-*` aliases, and avoid direct color literals outside the token layer.

## Positive evidence

- The theme primitives are genuine reusable Compose values, not screenshots or rasterized layout.
- All inspected authored color values are centralized in `Color.kt`; sizes are centralized in `Type.kt` rather than scattered through `App.kt`.
- Core readable pairs are strong: `YeobaekInk` on `YeobaekPaper` is 10.88:1 and `YeobaekTextSecondary` on `YeobaekPaper` is 5.44:1.
- Common metadata compilation succeeds.

## Approval blockers

1. Wire `YeobaekTheme` into the live common root used by Android and iOS.
2. Correct the failing `secondary/onSecondary` and `surfaceVariant/onSurfaceVariant` contrast pairs and verify their component usage.
3. Complete—or explicitly scope and test—the Material 3 role mappings so components cannot fall back to an unrelated default palette.
4. Supply a target design plus rendered screenshots if visual-fidelity approval is required.
