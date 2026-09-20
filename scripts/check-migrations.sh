#!/usr/bin/env bash
set -euo pipefail

migration_dir="${1:-src/main/resources/db/migration}"

if [[ ! -d "$migration_dir" ]]; then
  echo "Migration directory not found: $migration_dir" >&2
  exit 1
fi

files_found=0
version_index="$(mktemp)"
trap 'rm -f "$version_index"' EXIT
version_count=0

while IFS= read -r file; do
  files_found=1
  name="${file##*/}"

  if [[ ! "$name" =~ ^V([0-9]+)__[^/]+\.sql$ ]]; then
    echo "Invalid Flyway migration filename: $file" >&2
    exit 1
  fi

  version="${BASH_REMATCH[1]}"
  previous_file="$(awk -v version="$version" '$1 == version { print substr($0, index($0, $2)); exit }' "$version_index")"
  if [[ -n "$previous_file" ]]; then
    echo "Duplicate Flyway migration version V${version}: ${previous_file} and $file" >&2
    exit 1
  fi
  printf '%s %s\n' "$version" "$file" >> "$version_index"
  version_count=$((version_count + 1))

  if [[ ! -s "$file" ]]; then
    echo "Empty Flyway migration: $file" >&2
    exit 1
  fi

  # MySQL/PlanetScale rejects this form. Use a plain ADD COLUMN in a versioned
  # migration, or perform an explicit information_schema check when a migration
  # must be rerunnable manually.
  if grep -Eiq 'ALTER[[:space:]]+TABLE[^;]*ADD[[:space:]]+COLUMN[[:space:]]+IF[[:space:]]+NOT[[:space:]]+EXISTS' "$file"; then
    echo "MySQL-incompatible ADD COLUMN IF NOT EXISTS found in: $file" >&2
    exit 1
  fi
done < <(find "$migration_dir" -maxdepth 1 -type f -name 'V*.sql')

if [[ "$files_found" -eq 0 ]]; then
  echo "No Flyway migrations found in: $migration_dir" >&2
  exit 1
fi

echo "Migration checks passed (${version_count} files)."
