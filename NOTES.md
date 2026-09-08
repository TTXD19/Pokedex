# NOTES

## Assumptions and deviations (ambiguity handling)

The spec left gaps I couldn't ask about before building, so everything below
is assumed-and-documented:

- **Releasing a duplicate** — a capture is an *event* (own row + timestamp), and
  release removes the specific tapped capture, not every capture of that species.
- **Flavor text** — the API embeds Game Boy control characters (`\n`, `\f`). The
  mock appears to render them raw (odd line breaks); I treat that as a mock
  artifact, not intent, and collapse them to spaces.
- **Evolves-from outside the 151** — Pikachu → Pichu (#172), Jigglypuff →
  Igglybuff (#174) etc. point outside the 151. I initially rendered them
  non-tappable; that confused even me during testing, so Pokémon outside the 151
  are now fetched on demand for detail viewing while the collection stays
  strictly 151 (queries scope to `id <= 151`).
- **Tapping a captured Pokémon** — the spec gives the pocket card two jobs
  (tap to view details, release from the captured list) and the mock shows
  one card. A single tap can't mean both, and a bare Pokéball that releases
  on touch is easy to hit by accident, so tapping the card opens a
  "view details / release" dialog and release is styled as destructive.
  The Pokéball on pocket cards still releases directly, mirroring capture.
- **Back on the home screen asks before exiting** — not in the spec. Added
  because a stray back press during a long first sync would otherwise kill
  the app mid-fetch; the sync resumes on relaunch anyway, so this is about
  not surprising the user, not about protecting data.

## 1. Which parts did an AI tool write?

Most of the code was implemented with Claude. The split: I started by
discussing the requirements and the spec with it — what the mock implies,
where the text contradicts itself, what to assume; it then implemented the
modules (DAO, repository, network layer, ViewModels, screens); and I reviewed
each one, questioned what I didn't understand, and adjusted or rejected what
didn't hold up.

What I changed most often: rejecting output that was technically fine but
confusing (a capture/release panel on the detail screen became a dialog on
the pocket card), asking for simpler code where the robust version was harder
to read, and replacing its guesses with measurements (scroll performance).
The bugs it did introduce were the kind only a device catches — a Flow
`collect` that never returned, a refresh indicator stuck on cached data, a
missing `ACCESS_NETWORK_STATE` permission — so on-device verification was
where my time went.

## 2. Data model

```
pokemon        id (PK), name, imageUrl,
               detailFetched, speciesFetched,          ← fetch-state flags (detail: sync; species: lazily on first open)
               description, evolvesFromId, evolvesFromName
pokemon_types  (pokemonId, typeName) composite PK, slot, FK → pokemon, index(typeName)
captures       id (autoincrement PK), pokemonId FK → pokemon, capturedAt, index(pokemonId)
```

- **`pokemon`** — one row per Pokémon, keyed by the PokeAPI `id`. `name` and
  the row itself come from the list endpoint; `imageUrl` and the types come
  from the detail endpoint; `description` / `evolvesFrom*` from the species
  endpoint. Two flags decide what still needs fetching: `detailFetched = 0`
  puts the row on the sync queue, `speciesFetched = 0` makes the detail
  screen fetch species on first open. Whether the list itself needs
  refetching is `COUNT(*) WHERE id <= 151` being short of 151.
- **`pokemon_types`** — one row per (Pokémon, type) pair, keyed by the
  composite `(pokemonId, typeName)`; `slot` keeps the primary type first.
  It has no fetch flag of its own: it is rewritten (`REPLACE`) whenever its
  Pokémon's detail is fetched, so its freshness follows `pokemon.detailFetched`.
- **`captures`** — one row per capture event, keyed by its own autoincrement
  `id`, which is exactly what "release" deletes. `pokemonId` points at the
  species, `capturedAt` orders My Pocket. Nothing here is fetched: it is the
  user's local data. Sync never deletes `pokemon` rows (it only inserts-or-
  ignores and updates), which matters because a delete there would cascade
  into this table.

The decision that took longest was the `captures` table itself: a
`captured` flag or count on `pokemon` can't hold two Pikachu with different
timestamps and can't release just one of them, so a capture became its own
row. Everything else followed from that.

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

- **WorkManager / background sync.** Foreground sync + resume-on-next-launch
  covers the requirement; background scheduling adds surface without a stated
  need.
- **Force-refetch semantics for pull-to-refresh.** Data is static Gen-1; refresh
  re-checks and fetches only what's missing.
- **`evolution_chain` endpoint.** `evolves_from_species` alone covers the bonus;
  the full chain would double species-related requests for one extra hop.

## 5. What I like least / one more day

Least favorite: **the sync has no early abort.** If connectivity drops
mid-run, every remaining id still attempts and fails on its own timeout — the
UI stays usable, but it is a few minutes of futile requests. A
consecutive-failure circuit breaker is the fix and it did not make the cut.

With one more day that is what I would spend it on: stop the run after N
consecutive failures, surface "paused, will resume when back online" instead
of a failure count, and let the existing `connectivityRestored()` path pick
it up — the resume mechanism already exists, it just isn't told to wait.

## 6. Time: actual vs estimated

Estimated: **~2–3 part-time days**. Actual: **about 4 days at 2–3 hours
each**, Sep 5–8. The data layers went faster than expected; the extra day went
to on-device polish, the bugs only a device reveals, and a refactoring pass
after the first working build.
