#!/bin/bash
set -e

ORDER_FILE="/init-scripts/order.txt"
SCRIPTS_DIR="/init-scripts"

if [ -f "$ORDER_FILE" ]; then
    echo ">>> Reading script execution order from $ORDER_FILE..."

    # Read line by line, handling files that don't end in a newline character
    while IFS= read -r script_name || [ -n "$script_name" ]; do
        # Trim whitespace, skip empty lines, and skip comments starting with '#'
        script_name=$(echo "$script_name" | xargs)
        [[ -z "$script_name" || "$script_name" =~ ^# ]] && continue

        script_path="$SCRIPTS_DIR/$script_name"

        if [ -f "$script_path" ]; then
            echo ">>> Executing: $script_name"
            psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -f "$script_path"
        else
            echo ">>> WARNING: Script '$script_path' defined in order file was not found. Skipping."
        fi
    done < "$ORDER_FILE"

    echo ">>> All ordered scripts applied successfully."
else
    echo ">>> ERROR: Order file '$ORDER_FILE' not found!"
    exit 1
fi