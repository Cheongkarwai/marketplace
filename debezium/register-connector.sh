#!/usr/bin/env bash
set -e

CONNECT_URL="${CONNECT_URL:-http://localhost:8083}"
CONNECTOR_NAME="users-outbox-connector"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/register-postgres-outbox-connector.json"

case "$1" in
  status)
    echo "Checking status of ${CONNECTOR_NAME}..."
    curl -s "${CONNECT_URL}/connectors/${CONNECTOR_NAME}/status" | python3 -m json.tool || curl -s "${CONNECT_URL}/connectors/${CONNECTOR_NAME}/status"
    ;;
  register)
    echo "Registering ${CONNECTOR_NAME} to ${CONNECT_URL}..."
    curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" \
      "${CONNECT_URL}/connectors/" -d @"${CONFIG_FILE}"
    echo ""
    ;;
  update)
    echo "Updating configuration for ${CONNECTOR_NAME}..."
    CONFIG=$(python3 -c "import json; f=open('${CONFIG_FILE}'); print(json.dumps(json.load(f)['config'])); f.close()")
    curl -i -X PUT -H "Accept:application/json" -H "Content-Type:application/json" \
      "${CONNECT_URL}/connectors/${CONNECTOR_NAME}/config" -d "${CONFIG}"
    echo ""
    ;;
  restart)
    echo "Restarting ${CONNECTOR_NAME}..."
    curl -i -X POST "${CONNECT_URL}/connectors/${CONNECTOR_NAME}/restart"
    echo ""
    ;;
  delete)
    echo "Deleting ${CONNECTOR_NAME}..."
    curl -i -X DELETE "${CONNECT_URL}/connectors/${CONNECTOR_NAME}"
    echo ""
    ;;
  list)
    echo "Listing active connectors at ${CONNECT_URL}..."
    curl -s "${CONNECT_URL}/connectors"
    echo ""
    ;;
  *)
    echo "Usage: $0 {register|status|update|restart|delete|list}"
    exit 1
    ;;
esac
