# Phase 06 — Page Menu, Context Menus, Fast Toggles & Dialogs

> Goal: retire every legacy `BottomSheetDialog`/`AlertDialog` with a consistent M3 system: one redesigned
> page menu, link/list context menus, the fast-toggles panel, and all engine dialogs (SSL, auth,
> resubmission, download, save-as).
> Effort: 4–5 days · Depends on: P05 · Ships: release

## Steps

### 6.1 Page menu (replaces `dialog_menu_overflow`)
- [ ] Single scrollable M3 sheet — no more 4-FAB category switching. Order:
      New tab · Incognito (toggle w/ active state) · Find in page · ★ Set favorite · Add bookmark ·
      Add to Home · Create shortcut · Share · Copy link · Open with · Save as… · Print PDF · Downloads ·
      Settings · Quit.
- [ ] Header row: site title + security icon + **blocked-count badge** (from `AdBlockEngine.blockedCount`,
      new UX), reload button that preserves the HTTPS-upgrade prompt logic from `Reload()` (http→https
      confirm; https path = re-init preferences + recreate tab, parity kept).
- [ ] Incognito toggle here flips the *current session* semantics (P09 finalizes per-tab; interim keeps
      global key `IsIncognito` to avoid behavior drift).

### 6.2 Link context menu (long-press on web content)
- [ ] Port `show_contextMenu_link`: open in new tab (fg/bg), share, open-with, save-as, add-to-home;
      title shows target URL.

### 6.3 List context menus (bookmarks/history/home rows)
- [ ] Shared `RecordContextMenu`: new-tab fg/bg, delete (ConfirmSheet), edit (bookmarks only → title +
      ColorDotPicker, writes same DB fields incl. legacy color-code until P10).

### 6.4 Fast toggles panel (long-press menu button)
- [ ] Rebuild `dialog_toggle` as grid of toggle cards: JavaScript / AdBlock / Cookies / Remote — each card
      shows master switch **and** this-domain whitelist state chip (green check / red cross icons preserved
      semantically); tapping chip toggles domain membership via `WhitelistRepository`.
- [ ] Secondary row: history/location/images/invert quick switches + font shortcut + “apply & reload” action
      (recreates current tab after preference change, porting `ib_reload` behavior).

### 6.5 Engine dialog kit
- [ ] SSL-error dialog: clearer copy, show reason line (port the 6-case message builder), Proceed/Back;
      default-safe focus on Back.
- [ ] HTTP-auth dialog: user/pass fields, Cancel=handler.cancel().
- [ ] Form-resubmission: Resend/Cancel wired to the two `Message`s exactly.
- [ ] Download-confirm + Save-as (filename+extension validation rules identical, incl. “must start with dot”),
      storage-permission rationale flow preserved for pre-scoped devices (P09 revisits).
- [ ] Download-complete prompt (broadcast receiver) restyled; opens system downloads.

### 6.6 Toasts → snackbars
- [ ] Replace `Toasty` usages in new UI with snackbar host at app level; keep `Toasty` for remaining legacy
      paths until P11 deletion. Message strings reused unchanged.

### 6.7 Legacy cleanup (sheet-level)
- [ ] Delete now-unused builders from extracted `legacy/*` classes (P02): overview menu/sort/filter sheets,
      overflow menu, fast-toggle sheet, context menus, and their layouts (`dialog_menu*.xml`, `dialog_toggle.xml`,
      `dialog_action.xml`, `dialog_edit_title/edit_extension/edit_homescreen.xml`) — files dormant till P11,
      references removed now.

## Acceptance criteria
- [ ] Every action reachable from old page menu exists in new menu (checklist diff against
      `showOverflow()` item lists: Tab/Share/Save/Other × all items).
- [ ] All engine dialogs verified: bad-SSL site, basic-auth URL, POST resubmission, large-file download,
      save-as rename, download-complete notification tap.
- [ ] Fast toggles change engine behavior immediately (JS off blocks script on next load; whitelist chip
      reflects and persists).
- [ ] No orphaned bottom-sheet code paths remain callable.

## Rollback
Legacy sheets were deleted — rollback = revert branch (no data impact). Keep phase isolated for this reason.
