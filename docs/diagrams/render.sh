#!/usr/bin/env bash
# Regenerates local SVG and PNG exports. No source is sent to a diagram service.
# Requires Java on PATH, Arial, and the local PlantUML 1.2026.9 jar.
# Usage: PLANTUML_JAR="$HOME/tools/plantuml-1.2026.9.jar" bash docs/diagrams/render.sh
# Class views use built-in Smetana; Graphviz is optional.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
jar="${PLANTUML_JAR:?Set PLANTUML_JAR to your local PlantUML 1.2026.9 jar}"
if [[ ! -f "$jar" ]]; then
  echo "PlantUML jar not found: $jar" >&2
  exit 2
fi
# -version also probes optional Graphviz and can exit 16 without it; our class
# views use Smetana. Validate the reported version rather than that probe status.
version="$(java -jar "$jar" -version 2>&1 || true)"
if [[ "$version" != *"PlantUML version 1.2026.9 "* ]]; then
  echo "Use PlantUML 1.2026.9 for these checked-in exports." >&2
  exit 2
fi
export PLANTUML_LIMIT_SIZE=12000
sources=("$here"/*.puml)
build="$(mktemp -d "$here/.render.XXXXXX")"
cleanup() {
  # Only generated files in the verified staging directory; no recursive deletion.
  if [[ "$build" == "$here"/.render.* ]]; then
    rm -f -- "$build"/*.svg "$build"/*.png "$build"/*.tmp "$build/render.log"
    rmdir -- "$build" 2>/dev/null || true
  fi
}
trap cleanup EXIT
for format in svg png; do
  if ! java -Djava.awt.headless=true -jar "$jar" \
    -nometadata -charset UTF-8 -failfast2 "-t$format" \
    -o "$build" "${sources[@]}" > "$build/render.log" 2>&1; then
    cat "$build/render.log" >&2
    exit 1
  fi
  # Some output exceptions are logged without a nonzero PlantUML exit status.
  if grep -Eq 'Exception|Some diagram description contains errors' "$build/render.log"; then
    cat "$build/render.log" >&2
    exit 1
  fi
  for source in "${sources[@]}"; do
    stem="$(basename "$source" .puml)"
    if [[ ! -s "$build/$stem.$format" ]]; then
      echo "Missing or empty generated export: $stem.$format" >&2
      exit 1
    fi
    if [[ "$format" == svg ]]; then
      # Some SVG consumers ignore the root CSS background; make the canvas explicit.
      sed 's|<svg\([^>]*\)>|<svg\1><rect width="100%" height="100%" fill="#FFFFFF"/>|' \
        "$build/$stem.svg" > "$build/$stem.svg.tmp"
      mv -f -- "$build/$stem.svg.tmp" "$build/$stem.svg"
      if ! grep -Fq '<rect width="100%" height="100%" fill="#FFFFFF"/>' "$build/$stem.svg"; then
        echo "SVG background normalization failed: $stem.svg" >&2
        exit 1
      fi
    fi
  done
done
# Renaming fresh files avoids truncating an image that a Windows preview has mapped.
mkdir -p "$here/exports"
for source in "${sources[@]}"; do
  stem="$(basename "$source" .puml)"
  for format in svg png; do
    mv -f -- "$build/$stem.$format" "$here/exports/$stem.$format"
  done
done
echo "Rendered ${#sources[@]} UML sources as SVG and PNG in docs/diagrams/exports."
