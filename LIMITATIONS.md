# CloneKit v1 — Honest Limitations

This file lists what the Phase 1 repackage cloner **cannot** do. Read it before
testing, so a failed clone is understood instead of surprising.

## By design (v1 scope)

1. **No dex/smali patching.** Only the binary `AndroidManifest.xml` is modified.
   Apps that hardcode their package name in code, verify their own signature,
   or check `getPackageName()` against a server allow-list will install but
   crash, refuse to log in, or show errors. This is the single biggest cause of
   "clone doesn't work" and can only be fixed by Phase 2 (virtual engine) or
   full smali rewriting (heavy, fragile on-device).
2. **Split APKs (Android App Bundle) are rejected.** If an app was installed as
   multiple split APKs, CloneKit refuses with a clear message instead of
   building a broken clone. Merging splits is a Phase-2 task.
3. **No silent installs.** Android requires the user to tap “Install” in the
   system dialog for every clone (no root). 100 clones = 100 taps.
4. **Launcher label rename is best-effort.** The `<application android:label>`
   is overridden with a literal string. Activities with their own label, or
   launchers that cache names, may still show the old name.
5. **AXML styles are dropped.** String-pool styles (extremely rare in manifests)
   are not preserved by the rewriter.
6. **No ZIP64.** APKs are far below 4 GiB in practice; gigantic APKs would fail
   at the repack step.

## Will break in the clone (platform realities)

7. **Signature / integrity checks:** apps using Play Integrity, SafetyNet, or
   custom signature verification detect the re-sign and refuse to run. This
   includes most banking apps and many games.
8. **Google Sign-In:** the signing key (SHA-1) changed, so Google OAuth clients
   registered to the original key fail.
9. **Push notifications (FCM):** the clone's package/key no longer matches the
   Firebase project; notifications silently stop.
10. **Widgets, live wallpapers, accessibility services, device-admin features:**
    these bind to the original package identity and behave unpredictably in clones.
11. **Apps that share data via hardcoded authorities:** provider authorities are
    renamed (`…​.cloneN`); if the app builds authority strings at runtime from
    the *old* package name, its internal content-provider calls break.

## Distribution / policy

12. **Play Store publishing is unlikely.** Repackaging other developers' apps
    trips Play's IP and “dangerous functionality” review, and
    `QUERY_ALL_PACKAGES` + `REQUEST_INSTALL_PACKAGES` get strict scrutiny.
    Realistic distribution for v1 is direct APK (your own site / testers).
13. **OEM quirks:** some manufacturers (notably Xiaomi/MIUI with its own dual-app
    features, and devices with aggressive background killers) may interfere with
    installs or kill long clone jobs. The foreground-service setup mitigates but
    does not eliminate this.

## Performance

14. Cloning is CPU-heavy on-device (re-compress + sign a whole APK). Large apps
    (100 MB+) can take several minutes and noticeable battery on mid-range phones.
15. Each clone costs ~one APK size of storage (100 MB app × 10 clones ≈ 1 GB),
    plus the original.
