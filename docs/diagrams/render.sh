#!/usr/bin/env bash
# Regenerates local SVG and PNG exports. No source is sent to a diagram service.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
jar="${PLANTUML_JAR:?Set PLANTUML_JAR to your local PlantUML 1.2026.9 jar}"
if [[ ! -f "$jar" ]]; then
  echo "PlantUML jar not found: $jar" >&2
  exit 2
fi
version="$(java -jar "$jar" -version)"
if [[ "$version" != *"PlantUML version 1.2026.9 "* ]]; then
  echo "Use PlantUML 1.2026.9 for these checked-in exports." >&2
  exit 2
fi
export PLANTUML_LIMIT_SIZE=12000
for format in svg png; do
  java -Djava.awt.headless=true -jar "$jar" \
    -nometadata -charset UTF-8 -failfast2 "-t$format" \
    -o exports "$here"/*.puml
done
echo "Rendered six UML sources as SVG and PNG in docs/diagrams/exports."
