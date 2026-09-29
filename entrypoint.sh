#!/bin/sh
set -e

# Carga como variables de entorno cualquier Secret File montado por Render en /etc/secrets
SECRETS_DIR="/etc/secrets"
if [ -d "$SECRETS_DIR" ]; then
  set -a
  for file in "$SECRETS_DIR"/*; do
    [ -f "$file" ] && . "$file"
  done
  set +a
fi

exec java $JAVA_OPTS -jar /app/app.jar
