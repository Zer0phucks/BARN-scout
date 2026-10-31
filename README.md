# Scout App

Android property scouting app for VPT.

## Building

1. Open in Android Studio
2. Add your Google Maps API key to `local.properties`:
   ```
   MAPS_API_KEY=your_api_key_here
   ```
3. Build and run on device/emulator

## Configuration

Set these in `local.properties` (defaults live in `local.defaults.properties` and are exposed as `BuildConfig` fields):

```
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_ANON_KEY=<publishable key>
WEB_APP_URL=https://<scanner-webgui>.vercel.app
```

## Sign-in

- Accounts are invite-only. An admin invites the user from the Supabase dashboard
  (Auth → Users → Invite), then approves them:
  `insert into public.user_roles (user_id, role) values ('<auth user id>', 'scout');`
- Signing in calls `has_app_access()`. Accounts without an `admin` or `scout` role are rejected,
  and the check runs again on each app start so revoked users are signed out.
- "Forgot password?" emails a link to `WEB_APP_URL/reset-password`, which is also where invite
  links let new users set a password.
- Sign out from the Stats tab (top-right icon). Tokens are kept out of device backups.

## Release signing

Release builds are signed from `keystore.properties` (gitignored), which points at a keystore kept outside the repo:

```
storeFile=/home/<you>/.android-keys/barn-scout-release.jks
storePassword=...
keyAlias=barn-scout
keyPassword=...
```

Do not put these in `local.properties`: the Maps secrets plugin copies every `local.properties` entry into `BuildConfig`, which ships inside the APK. Without `keystore.properties`, `assembleRelease` produces an unsigned APK.

Back up the keystore and its passwords. Every update must be signed with the same key, or installed apps can't be upgraded. Certificate SHA-256: `46:0B:0A:CD:6C:04:34:0F:56:FD:C4:A9:D3:3D:22:86:3B:3B:DF:50:65:8F:00:62:56:04:1D:EB:40:D3:6B:62`.

Build with `./gradlew assembleRelease`, then upload `app/build/outputs/apk/release/app-release.apk` over `downloads/barn-scout.apk` in Supabase Storage (linked from the web login page).
