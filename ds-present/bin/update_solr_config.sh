#!/usr/bin/env bash
#
# update_solr_config.sh
#
# Fetches the ds-present bin scripts and Solr configuration from the
# kb-dk/ds-backend GitHub repository for a given branch, and copies them
# into the local solr-management folders (overwriting existing files).
#
# Only the two needed folders are downloaded (sparse checkout + partial clone).
#
# Usage: update_solr_config.sh <branch>
# Example: update_solr_config.sh main

set -euo pipefail

# --- Configuration ----------------------------------------------------------
REPO_URL="https://github.com/kb-dk/ds-backend.git"

BIN_SOURCE="ds-present/bin"
BIN_TARGET="/home/digisam/solr-management/bin"

SOLR_SOURCE="ds-present/src/main/solr"
SOLR_TARGET="/home/digisam/solr-management/solr"
# -----------------------------------------------------------------------------

die() {
  echo "ERROR: $*" >&2
  exit 1
}

# --- Validate arguments -----------------------------------------------------
if [[ $# -ne 1 || -z "${1:-}" ]]; then
  echo "Usage: $(basename "$0") <branch>" >&2
  die "No branch specified."
fi
BRANCH="$1"

command -v git >/dev/null 2>&1 || die "git is not installed."

# --- Check that the branch exists on GitHub ---------------------------------
echo "Checking that branch '$BRANCH' exists on $REPO_URL ..."
set +e
git ls-remote --exit-code --heads "$REPO_URL" "refs/heads/$BRANCH" >/dev/null 2>&1
rc=$?
set -e
case $rc in
  0) ;;  # branch found
  2) die "Branch '$BRANCH' does not exist on $REPO_URL" ;;
  *) die "Could not contact $REPO_URL (git ls-remote exit code $rc)." ;;
esac

# --- Sparse checkout into a temporary directory -----------------------------
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

echo "Fetching '$BIN_SOURCE' and '$SOLR_SOURCE' from branch '$BRANCH' ..."
git clone --quiet --filter=blob:none --no-checkout --depth 1 \
  --branch "$BRANCH" "$REPO_URL" "$WORK_DIR/repo"
git -C "$WORK_DIR/repo" sparse-checkout set --cone "$BIN_SOURCE" "$SOLR_SOURCE"
git -C "$WORK_DIR/repo" checkout --quiet "$BRANCH"

for src in "$BIN_SOURCE" "$SOLR_SOURCE"; do
  [[ -d "$WORK_DIR/repo/$src" ]] \
    || die "Folder '$src' does not exist in branch '$BRANCH'."
done

# --- Copy to target folders --------------------------------------------------
install_dir() {
  local src="$1" dest="$2"
  echo "Copying $src -> $dest"
  mkdir -p "$dest"
  # Recursive copy, overwrites existing files, preserves permissions (exec bits).
  cp -a "$WORK_DIR/repo/$src/." "$dest/"
}

install_dir "$BIN_SOURCE"  "$BIN_TARGET"
install_dir "$SOLR_SOURCE" "$SOLR_TARGET"

COMMIT="$(git -C "$WORK_DIR/repo" rev-parse --short HEAD)"
echo "Done. Installed from branch '$BRANCH' (commit $COMMIT)."
