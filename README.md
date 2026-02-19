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

Set values in `app/src/main/res/values/strings.xml`:

- `api_base_url` to your scanner API base URL (for emulator local Flask use `http://10.0.2.2:5000`)
- `supabase_url` and `supabase_anon_key` to the same Supabase project used by BARN admin

The app now signs in with Supabase email/password and sends `Authorization: Bearer <token>` to scanner API endpoints.
