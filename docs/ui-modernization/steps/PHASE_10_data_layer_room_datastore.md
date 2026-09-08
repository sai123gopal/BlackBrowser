# Phase 10 — Data Layer: Room + DataStore + SAF Backup

> Goal: replace raw SQLite/`SharedPreferences` access with Room + DataStore **in place** (no user-visible
> data loss), fix the bookmark-color schema abuse (B6), and make backup/restore robust with SAF (B9).
> Effort: 5–7 days · Depends on: P02 (repositories exist as wrappers) · Ships: release (data-gated rollout)

## Steps

### 10.1 Room over the existing `data.db`
- [ ] Entities mirror today’s tables exactly (names/columns preserved): `HistoryRow`, `BookmarkRow`,
      `GridTileRow`, `DomainRow(kind)` for WHITELIST/JAVASCRIPT/COOKIE/REMOTE, `TabSessionRow` (activates TAB).
- [ ] Database config:
```kotlin
@Database(version = 5, exportSchema = true)
abstract class BBDatabase : RoomDatabase() { … }
Room.databaseBuilder(ctx, BBDatabase::class.java, "data.db")   // same file → in-place upgrade
    .addMigrations(MIGRATION_4_5)
    .build()
```
- [ ] `MIGRATION_4_5`:
```sql
ALTER TABLE BOOKMARK ADD COLUMN COLOR INTEGER NOT NULL DEFAULT 0;
UPDATE BOOKMARK SET COLOR = TIME, TIME = <nowMs> WHERE TIME BETWEEN 1 AND 11;  -- legacy color hack (B6)
-- TAB table already exists from v3/v4 CREATE; add nothing, just start using it
```
- [ ] Migration test: ship a v4 fixture DB in androidTest assets; run migration; assert colors moved and
      epoch times untouched.
- [ ] DAOs suspend/Flow; repositories switch internals to DAOs — public repository API unchanged (UI untouched).
- [ ] Delete `RecordAction`, `RecordHelper`, `BookmarkList`, `RecordUnit` string-concat SQL (B7) after parity tests.

### 10.2 DataStore preferences migration
- [ ] `SettingsRepository` backed by `preferencesDataStore(name = "settings_v2")`; typed keys defined once
      (`BBKeys`) — names may be new/clean since mapping is internal.
- [ ] One-time importer on first run: read every legacy key (Appendix A master plan) → write DataStore →
      set `legacy_prefs_imported=true`; keep reading SharedPreferences only until import completes.
- [ ] Home-tile keys `{0..7}_{name,url}` imported into GRID during this step (if P07 guard hasn’t already) —
      idempotent by URL dedupe.
- [ ] Kill restart-required flows permanently: search engine/UA changes emit new state instantly (consumers
      re-init engine settings on next navigation).

### 10.3 Backup & restore via SAF
- [ ] New formats (versioned JSON): full profile = prefs snapshot + bookmarks + tiles + whitelists(+kinds) +
      sessions. Actions use `ActivityResultContracts.CreateDocument("application/json")` / `OpenDocument`.
- [ ] Compat exports kept byte-compatible where cheap: bookmarks Netscape HTML (`export_Bookmarks.html`
      format strings ported), whitelist txt files, so users can round-trip with old versions.
- [ ] Remove hardcoded `/storage/emulated/0/BlackBrowser_backup` writes & the `/document/primary:` path
      rewrite hack (B9). Optional convenience: default suggested filename/dir via `DocumentsContract`.
- [ ] Settings screens (P03 “legacy” links) now point at new actions; delete `FragmentSettingsData` +
      `ExportWhiteListTask`/`ImportWhitelistTask` AsyncTask classes.

### 10.4 Session persistence activation
- [ ] `TabManager` writes rows to TAB on open/close/title-change (debounced 500 ms); restore honors pref
      “Continue where you left off” (new, default ON replacing `start_tabStart` semantics carefully — keep old
      pref functioning if new one absent).
- [ ] Incognito tabs never persisted.

### 10.5 Cleanup
- [ ] Remove `pass_DB_v01` one-shot migration code entirely (marker key stays forever).
- [ ] Drop legacy `SharedPreferences` reads after import flag verified across a beta cohort (keep writer for
      one release as safety net, then delete in P11).

## Acceptance criteria
- [ ] Upgrade test matrix: v1.5.43 install → apply update build → all history/bookmarks/tiles/whitelists/
      settings intact; bookmark colors correct after B6 repair; repeat with interrupted-first-launch case.
- [ ] Backup → fresh-install restore produces identical state (JSON diff empty except timestamps).
- [ ] Netscape HTML exported by OLD version imports in NEW, and vice-versa.
- [ ] No `.allowMainThreadQueries()` anywhere; strict-mode clean on debug builds.

## Rollback
Data migrations are forward-only: before MIGRATION_4_5 runs, auto-copy `data.db` to
`files/pre_upgrade_backup.db` (kept 30 days). Rollback release reads it back via a compat shim if ever needed.
