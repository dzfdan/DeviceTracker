# DeviceTracker Chinese Localization Design

## Objective

Convert the current DeviceTracker Android app UI to polished Simplified Chinese with a product-style tone, while preserving the existing visual redesign and app structure.

## Scope

This work includes:

- Translating user-visible copy in `app/src/main/res/values/strings.xml`
- Reviewing `app/src/main/res/values/plurals.xml` for user-visible quantity strings
- Replacing user-visible hardcoded English strings in `MainActivity.kt`
- Replacing user-visible hardcoded English strings in `DeviceTrackActivity.kt`
- Keeping existing layouts, flows, and behavior unchanged unless text resource migration is required

This work does not include:

- Building a multi-language resource strategy with `values-zh`
- Back-end or data-layer localization
- Renaming package names, classes, or technical identifiers
- Reworking screens beyond text changes needed for localization

## Chosen Approach

Use the existing default resource set as the single source of truth and convert it to Simplified Chinese with a productized tone.

The implementation will:

- Update the existing default string resources directly instead of introducing parallel language folders
- Move remaining hardcoded user-facing copy into string resources when practical
- Keep copy concise, product-like, and consistent across map, list, track, permission, empty, and error states

This approach was chosen because the current goal is full Chinese productization of the shipped UI, not long-term multilingual support.

## Copy Style

Copy should follow these rules:

- Use Simplified Chinese throughout
- Favor natural product wording over literal translation
- Keep operational labels short and scannable
- Keep status, permission, and error messaging calm and actionable
- Preserve technical precision where the user needs it, such as counts, distances, timestamps, and failure reasons

Examples of the intended tone:

- `Grant` becomes `去授权` or `授权`
- `Try again` becomes `重新加载` or `重试`, based on context
- `Fleet roster unavailable` becomes a natural Chinese failure state rather than a direct literal translation

## Resource Strategy

Primary resource updates will happen in `strings.xml`.

Requirements:

- Preserve all existing string keys unless a key is clearly unused and removable as part of the touched code path
- Preserve formatting placeholders and convert them safely where Chinese word order requires it
- Keep apostrophe and format syntax valid for Android resource compilation
- Keep any copy used in multiple surfaces centralized in resources

`plurals.xml` will be checked for quantity strings that remain visible after the redesign and adjusted to a natural Chinese form where needed.

## Code Changes

The code-side localization pass should be minimal and limited to user-visible hardcoded strings.

Known targets include:

- Permission denial `Toast` messages in `MainActivity.kt`
- Waiting and failure `Toast` messages in `MainActivity.kt`
- Invalid device ID `Toast` in `DeviceTrackActivity.kt`
- Track marker titles and snippets in `DeviceTrackActivity.kt`

Preferred implementation order:

1. Add or update string resources
2. Replace hardcoded literals with `getString(...)`
3. Keep all existing control flow unchanged

## Quality Bar

The result is successful when:

- No user-visible English copy remains in the main app surfaces touched by this redesign pass
- Resource compilation still succeeds
- Existing placeholders, counts, and distances display correctly
- The updated copy reads like a finished Chinese product rather than a direct translation draft

## Verification

Verification will include:

- Running `:app:processDebugResources` using the repaired Gradle wrapper
- Running a broader debug build if the environment allows it
- Grepping the touched app sources for remaining known user-visible English literals

## Risks

- Chinese copy may become too long for some controls, especially buttons and summary labels
- Placeholder order may break if format strings are translated carelessly
- Some user-visible English may remain hidden in less obvious code paths outside the currently identified files

Mitigations:

- Prefer short labels where space is constrained
- Use positional placeholders when translation changes word order
- Run targeted searches after the edit to catch leftovers
