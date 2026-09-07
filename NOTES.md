# NOTES

## Assumptions and deviations (ambiguity handling)

The spec left gaps; the process change meant I couldn't ask, so everything below
is assumed-and-documented:

- **"Sorted by ID" vs "sorted by alphabet"** — the requirement text says both.
  The mock resolves it: type sections alphabetical (Bug → Dragon → Electric),
  Pokémon within a section by id (Caterpie #10 → Weedle #13). I followed the mock.
- **Releasing a duplicate** — a capture is an *event* (own row + timestamp), and
  release removes the specific tapped capture, not every capture of that species.
- **Flavor text** — the API embeds Game Boy control characters (`\n`, `\f`). The
  mock appears to render them raw (odd line breaks); I treat that as a mock
  artifact, not intent, and collapse them to spaces.
- **Purple status bar / app bar (deviation from mock)** — home has no app bar, and
  a solid purple strip over the status area looked wrong on tall-status-bar
  devices (foldables), so home is white edge-to-edge; the detail app bar followed
  for consistency. I read the mock's intent as "a coherent look", not "this hex".
- **Evolves-from outside the 151** — Pikachu → Pichu (#172), Jigglypuff →
  Igglybuff (#174) etc. point outside the 151. I initially rendered them
  non-tappable; that confused even me during testing, so Pokémon outside the 151
  are now fetched on demand for detail viewing while the collection stays
  strictly 151 (queries scope to `id <= 151`).
- **API politeness** — the spec invites asking how hard to hit PokeAPI; with
  nobody to ask I capped detail fetches at 5 concurrent, no retry storms, and
  cache everything forever (Gen-1 data is static; the API itself sends
  `cache-control: max-age=86400`).

## 1. Which parts did an AI tool write?

Nearly all of the code was written with an AI assistant (Claude), with me
directing the architecture, reviewing every layer, and testing on device. The
interesting part is what the AI got wrong and how it was caught:

- **`ensureSpecies` first draft** misused a Flow to read one-shot state
  (`collect` with an early return that never terminates) — it would have hung
  forever. Replaced with a one-shot DAO query. Caught in review before it ran.
- **Pull-to-refresh first draft** bound the indicator to the global sync state.
  On device the indicator got stuck forever: when everything is cached, sync
  finishes in milliseconds — faster than the indicator's show/hide animation.
  Rewrote it with a ViewModel-owned `isRefreshing` flag and a 400 ms floor.
  Caught only by running the app.
- **`NetworkMonitor` first draft crashed on device** — `registerNetworkCallback`
  needs `ACCESS_NETWORK_STATE`, which the manifest didn't declare. A
  `SecurityException` that no amount of compilation catches.

The pattern: the AI's output compiles and looks plausible; the value I added was
insisting on on-device verification and tests that could actually fail.

## 2. Data model

```
pokemon        id (PK), name, imageUrl,
               detailFetched, speciesFetched,          ← resumable-sync flags
               description, evolvesFromId, evolvesFromName
pokemon_types  (pokemonId, typeName) composite PK, FK → pokemon, index(typeName)
captures       id (autoincrement PK), pokemonId FK → pokemon, capturedAt
```

**The decision that took longest: captures as events, not a flag.** The obvious
model is `captured: Boolean` (or a count) on `pokemon`. It breaks three
requirements at once: duplicates in My Pocket, ordering by capture time, and
releasing one specific capture. Once capture is a timestamped row, all three
fall out for free, and "release" is `DELETE WHERE id = ?`.

Runner-up: **fetch-state flags live in the `pokemon` row.** That's the entire
resume mechanism — after process death, `WHERE detailFetched = 0` *is* the work
queue. The alternative (in-memory progress tracking) would have needed separate
persistence and could drift from the data it describes.

Late change: Pokémon outside the 151 share the `pokemon` table, and membership
in the 151 is expressed as `id <= 151` in the collection/sync queries rather
than a schema column. That leans on the 151 being a fixed id-prefix (true for
this assignment's endpoint); a non-contiguous list would break it, and then
I'd add an `inList` column with a one-line migration.

## 3. The requirement I was least confident about

**"Can be continued after an unexpected interruption"** — resumability is easy
to believe and hard to know. What I actually did:

- Unit tests run the real `PokemonRepositoryImpl` against programmable fakes:
  seeded a DB that "died" at 100/151, ran sync, asserted the API received
  exactly the 51 missing ids and zero list refetch; fully-synced → zero
  requests; 7 injected failures → `Failed(7)` with the other 144 landed;
  retry → exactly those 7; peak in-flight concurrency == 5.
- On device: killed the app mid-sync and relaunched (fetch continued from the
  gap); watched logcat with a dedicated `PokeApi` tag — a fresh install logs
  exactly 1 list + 151 detail requests, and pull-to-refresh afterwards logs
  none.
- "Display content as soon as fetched" was verified by screenshotting mid-sync:
  section counts visibly grow (Bug 10→12) instead of appearing all at once.

## 4. What I decided not to build

- **Sync early-abort when connectivity drops mid-run.** Known weakness: on a
  slow-timeout network the remaining ids each still attempt and fail (worst
  case a few minutes of futile requests, UI stays usable). Kept because the
  trigger is narrow; fix would be a consecutive-failure circuit breaker.
- **WorkManager / background sync.** Foreground sync + resume-on-next-launch
  covers the requirement; background scheduling adds surface without a stated
  need.
- **Force-refetch semantics for pull-to-refresh.** Data is static Gen-1; refresh
  re-checks and fetches only what's missing.
- **Captured-state visuals in the collection, release undo/confirm.** Cut for
  scope; capture events make them cheap to add later.
- **Tablet layouts, dark theme, i18n.** Responsive-by-lists only; light theme;
  English (matches the mock).
- **`evolution_chain` endpoint.** `evolves_from_species` alone covers the bonus;
  the full chain would double species-related requests for one extra hop.

## 5. What I like least / one more day

Least favorite: **everything above the ViewModel is only manually verified.**
The 22 unit tests stop at the ViewModel boundary; there are no Compose UI tests,
and the DAO's SQL (ordering, the `id <= 151` scoping) is exercised only through
fakes that *mirror* its semantics rather than Room itself. With one more day
I'd add Robolectric + in-memory-Room tests for the DAO queries and a Compose
test for capture → appears in My Pocket → release.

Second: the reconnect-triggers-reload logic is duplicated in both ViewModels;
it wants to be one shared observer.

## 6. Time: actual vs estimated

The planned scope discussion didn't happen (process was adjusted to
submit-before-interview), so the estimate was my own: **~2–3 part-time days**.
Actual: **~3 part-time days** across Sep 5–7, roughly on target, but the time
did not go where I expected:

- Layers I expected to dominate (API/DB/repository/ViewModel) went fast —
  roughly a third of the time, AI assistance at its most effective.
- The unplanned majority went to **on-device polish and the bugs only devices
  reveal**: the stuck refresh indicator, the missing network permission,
  edge-to-edge insets across three screen shapes (phone/foldable/landscape
  cutouts), and the outside-the-151 evolves-from rabbit hole — which started as
  "why can't I tap Igglybuff" and ended as a scope decision, a data-boundary
  design, and a UX affordance fix.

An unfinished part I can explain beats a finished one I can't; the list in
section 5 is exactly that.
