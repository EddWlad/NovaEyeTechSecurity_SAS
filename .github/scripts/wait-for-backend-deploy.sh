#!/usr/bin/env bash
# Espera al despliegue del backend del MISMO commit antes de desplegar el frontend.
#
# Un frontend nuevo contra un backend viejo deja pantallas sin endpoint (el dashboard llama a
# /dashboard/summary), y los dos workflows arrancan a la vez con el mismo push. Este script fija el
# orden: backend primero.
#
# Solo un fallo CONFIRMADO del backend detiene el despliegue (exit 1). Cualquier otra duda (la API de
# GitHub no responde, no hay permiso, se agota la espera) deja continuar: es preferible no bloquear un
# despliegue por un problema del propio chequeo.
#
# Entorno: GH_TOKEN, GITHUB_REPOSITORY, GITHUB_SHA. Opcionales (para pruebas): INITIAL_WAIT_SECONDS,
# POLL_SECONDS, MAX_ATTEMPTS.
set -u

INITIAL_WAIT_SECONDS="${INITIAL_WAIT_SECONDS:-20}"
POLL_SECONDS="${POLL_SECONDS:-10}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-60}"

# Margen para que el workflow del backend ya figure en la lista cuando se consulte.
sleep "$INITIAL_WAIT_SECONDS"

for ((attempt = 1; attempt <= MAX_ATTEMPTS; attempt++)); do
  if ! RUN=$(gh run list \
      --repo "$GITHUB_REPOSITORY" \
      --workflow backend-deploy.yml \
      --commit "$GITHUB_SHA" \
      --limit 1 \
      --json status,conclusion \
      --jq '.[0] | select(. != null) | "\(.status) \(.conclusion)"' 2>/dev/null); then
    echo "No se pudo consultar el despliegue del backend; se continua."
    exit 0
  fi

  case "$RUN" in
    "")
      echo "Este commit no despliega el backend; se continua."
      exit 0
      ;;
    "completed success")
      echo "Backend desplegado; se continua con el frontend."
      exit 0
      ;;
    completed*)
      echo "El despliegue del backend termino en '${RUN#completed }': se cancela el del frontend."
      exit 1
      ;;
  esac

  echo "Esperando al backend (${RUN% }) - intento $attempt de $MAX_ATTEMPTS..."
  sleep "$POLL_SECONDS"
done

echo "Se agoto la espera del backend; se continua."
exit 0
