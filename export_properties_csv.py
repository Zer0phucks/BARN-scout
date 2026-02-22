#!/usr/bin/env python3
"""
Export all property addresses from Supabase to a Google My Maps-compatible CSV.

This script connects directly to the Supabase Postgres database.
It requires the database password, which you can get from:
  https://supabase.com/dashboard/project/vzgmmlaojvkpbakvgcwh/settings/database

Usage:
    export SUPABASE_DB_PASSWORD="your-password-here"
    python export_properties_csv.py

    -- OR run directly if you paste the password below --

Output: properties_for_google_maps.csv

📍 Import into Google My Maps:
   1. Go to https://www.google.com/maps/d/
   2. Create a new map → Add Layer → Import
   3. Upload properties_for_google_maps.csv
   4. Position columns: Latitude & Longitude
   5. Title column: Name → Done!
"""

import csv
import json
import math
import os
import sys

# ── Config ───────────────────────────────────────────────────────────────────
DB_HOST     = "db.vzgmmlaojvkpbakvgcwh.supabase.co"
DB_PORT     = 5432
DB_NAME     = "postgres"
DB_USER     = "postgres"
DB_PASSWORD = os.environ.get("SUPABASE_DB_PASSWORD", "")

OUTPUT_FILE = "properties_for_google_maps.csv"

# ── Coordinate conversion ─────────────────────────────────────────────────────
def mercator_to_latlng(x: float, y: float):
    """Web Mercator EPSG:3857 → WGS84 lat/lon."""
    lon = (x / 20037508.342) * 180.0
    lat = math.degrees(2 * math.atan(math.exp((y / 20037508.342) * math.pi)) - math.pi / 2)
    return round(lat, 7), round(lon, 7)

# ── Main ──────────────────────────────────────────────────────────────────────
def main():
    try:
        import psycopg2
        import psycopg2.extras
    except ImportError:
        print("ERROR: psycopg2 not installed.")
        print("Run: pip install psycopg2-binary")
        sys.exit(1)

    if not DB_PASSWORD:
        print("ERROR: SUPABASE_DB_PASSWORD environment variable not set.")
        print()
        print("Get your database password from:")
        print("  https://supabase.com/dashboard/project/vzgmmlaojvkpbakvgcwh/settings/database")
        print()
        print("Then run:")
        print("  export SUPABASE_DB_PASSWORD='your-password'")
        print("  python export_properties_csv.py")
        sys.exit(1)

    print(f"Connecting to {DB_HOST}…")
    try:
        conn = psycopg2.connect(
            host=DB_HOST, port=DB_PORT, dbname=DB_NAME,
            user=DB_USER, password=DB_PASSWORD, sslmode="require"
        )
    except Exception as e:
        print(f"Connection failed: {e}")
        sys.exit(1)

    cur = conn.cursor(cursor_factory=psycopg2.extras.DictCursor)
    print("Querying properties…")

    cur.execute("""
        SELECT
            b.apn,
            b.location_of_property  AS address,
            b.city,
            b.has_vpt,
            b.condition_score,
            (p.row_json::json->>'SitusZip')    AS zip,
            (p.row_json::json->>'CENTROID_X')  AS cx,
            (p.row_json::json->>'CENTROID_Y')  AS cy
        FROM bills b
        LEFT JOIN parcels p ON b.apn = p."APN"
        WHERE b.location_of_property IS NOT NULL
          AND b.location_of_property <> ''
        ORDER BY b.city, b.location_of_property;
    """)

    rows = cur.fetchall()
    print(f"Found {len(rows)} properties.")

    written = 0
    with open(OUTPUT_FILE, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        # Google My Maps recognises "Latitude", "Longitude", and "Name"
        w.writerow(["Name", "Description", "Latitude", "Longitude", "Full Address",
                    "APN", "City", "Has VPT", "Condition Score"])

        for row in rows:
            apn     = row["apn"] or ""
            address = (row["address"] or "").strip()
            city    = (row["city"] or "").strip()
            has_vpt = row["has_vpt"] or 0
            cond    = row["condition_score"]
            zip_c   = row["zip"] or ""
            cx, cy  = row["cx"], row["cy"]

            if not address:
                continue

            # Convert parcel centroid to lat/lon
            lat = lon = ""
            if cx and cy:
                try:
                    lat, lon = mercator_to_latlng(float(cx), float(cy))
                except (ValueError, TypeError):
                    pass

            # Build a geocodable full address
            parts = [address]
            if city and city.upper() not in address.upper():
                parts.append(city)
            if zip_c:
                parts.append(zip_c)
            parts.append("CA")
            full_address = ", ".join(parts)

            desc = f"APN: {apn}"
            if has_vpt:
                desc += " | HAS VPT"
            if cond:
                desc += f" | Cond: {cond}"

            w.writerow([address, desc, lat, lon, full_address,
                        apn, city, "Yes" if has_vpt else "No", cond or ""])
            written += 1

    cur.close()
    conn.close()

    print(f"\n✅  Done!  {written:,} properties → {OUTPUT_FILE}")
    print()
    print("📍  Import into Google My Maps:")
    print("    1. Go to https://www.google.com/maps/d/")
    print("    2. Create a new map → click 'Add Layer' → 'Import'")
    print("    3. Upload  properties_for_google_maps.csv")
    print("    4. Position columns → Latitude & Longitude")
    print("    5. Title column → Name   →  Done!")

if __name__ == "__main__":
    main()
