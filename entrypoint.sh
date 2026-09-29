#!/bin/sh
set -e

# Carga como variables de entorno cualquier Secret File montado por Render en /etc/secrets
SECRETS_DIR="/etc/secrets"
if [ -d "$SECRETS_DIR" ]; then
  set -a
  for file in "$SECRETS_DIR"/*; do
    if [ -f "$file" ]; then
      # /etc/secrets es de solo lectura y puede venir con CRLF; se limpia en una copia temporal antes de cargarla
      tmp_file="/tmp/$(basename "$file").sh"
      tr -d '\r' < "$file" > "$tmp_file"
      . "$tmp_file"
      rm -f "$tmp_file"
    fi
  done
  set +a
fi

exec java $JAVA_OPTS -jar /app/app.jar
