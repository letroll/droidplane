#!/usr/bin/env bash
# =============================================================================
# package-appimage.sh — Build a Linux AppImage from a Compose Desktop distributable
#
# Usage:
#   ./scripts/package-appimage.sh [OPTIONS]
#
# Options:
#   --version    <version>   App version (default: extracted from app/build.gradle.kts)
#   --dist-dir   <dir>       Path to createDistributable output dir
#                            (default: app/build/compose/binaries/main/app/Droidplane)
#   --output-dir <dir>       Directory where the .AppImage will be written
#                            (default: app/build/compose/binaries/main/appimage)
#   --appimagetool <path>    Path to appimagetool binary (default: auto-detect/download)
#   -h, --help               Show this help message
#
# Requirements:
#   - bash 4+, cp, ln, chmod, mkdir, curl (for auto-download fallback)
#   - appimagetool (auto-downloaded if absent)
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Defaults
# ---------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

DEFAULT_DIST_DIR="${REPO_ROOT}/app/build/compose/binaries/main/app/Droidplane"
DEFAULT_OUTPUT_DIR="${REPO_ROOT}/app/build/compose/binaries/main/appimage"
DEFAULT_APPIMAGETOOL=""  # Empty → auto-detect

VERSION=""
DIST_DIR="${DEFAULT_DIST_DIR}"
OUTPUT_DIR="${DEFAULT_OUTPUT_DIR}"
APPIMAGETOOL_PATH="${DEFAULT_APPIMAGETOOL}"

# ---------------------------------------------------------------------------
# Logging helpers
# ---------------------------------------------------------------------------
info()  { echo "[INFO]  $*"; }
ok()    { echo "[OK]    $*"; }
warn()  { echo "[WARN]  $*" >&2; }
error() { echo "[ERROR] $*" >&2; exit 1; }

# ---------------------------------------------------------------------------
# Argument parsing
# ---------------------------------------------------------------------------
usage() {
  grep '^#' "${BASH_SOURCE[0]}" | head -n 30 | sed 's/^# \{0,1\}//'
  exit 0
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --version)      VERSION="$2";         shift 2 ;;
    --dist-dir)     DIST_DIR="$2";        shift 2 ;;
    --output-dir)   OUTPUT_DIR="$2";      shift 2 ;;
    --appimagetool) APPIMAGETOOL_PATH="$2"; shift 2 ;;
    -h|--help)      usage ;;
    *) error "Unknown option: $1. Use --help for usage." ;;
  esac
done

# ---------------------------------------------------------------------------
# Resolve version from build.gradle.kts if not provided
# ---------------------------------------------------------------------------
if [[ -z "${VERSION}" ]]; then
  GRADLE_KTS="${REPO_ROOT}/app/build.gradle.kts"
  if [[ ! -f "${GRADLE_KTS}" ]]; then
    error "Cannot find app/build.gradle.kts to extract version. Use --version."
  fi
  VERSION="$(awk -F'"' '/val appVersionName\s*=/{print $2}' "${GRADLE_KTS}" | head -n1)"
  if [[ -z "${VERSION}" ]]; then
    error "Failed to extract appVersionName from ${GRADLE_KTS}. Use --version."
  fi
  info "Version extracted from build.gradle.kts: ${VERSION}"
fi

# ---------------------------------------------------------------------------
# Validate distributable directory
# ---------------------------------------------------------------------------
if [[ ! -d "${DIST_DIR}" ]]; then
  error "Distributable directory not found: ${DIST_DIR}
Run './gradlew :app:createDistributable' first."
fi

if [[ ! -f "${DIST_DIR}/bin/Droidplane" ]]; then
  error "Launcher not found at ${DIST_DIR}/bin/Droidplane.
Is the distributable complete?"
fi

# ---------------------------------------------------------------------------
# Locate or download appimagetool
# ---------------------------------------------------------------------------
find_appimagetool() {
  # 1. Explicit path provided
  if [[ -n "${APPIMAGETOOL_PATH}" ]]; then
    if [[ -x "${APPIMAGETOOL_PATH}" ]]; then
      echo "${APPIMAGETOOL_PATH}"; return 0
    else
      error "appimagetool not executable at: ${APPIMAGETOOL_PATH}"
    fi
  fi

  # 2. Standard locations
  for candidate in /usr/local/bin/appimagetool /usr/bin/appimagetool; do
    if [[ -x "${candidate}" ]]; then
      echo "${candidate}"; return 0
    fi
  done

  # 3. PATH
  if command -v appimagetool &>/dev/null; then
    echo "$(command -v appimagetool)"; return 0
  fi

  # 4. Download fallback
  warn "appimagetool not found — downloading from AppImageKit releases..."
  local download_path="/tmp/appimagetool-x86_64.AppImage"
  local url="https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage"
  if ! command -v curl &>/dev/null; then
    error "curl is required to download appimagetool. Install it or provide --appimagetool."
  fi
  curl -fsSL -o "${download_path}" "${url}" || error "Failed to download appimagetool from ${url}"
  chmod +x "${download_path}"
  echo "${download_path}"
}

APPIMAGETOOL="$(find_appimagetool)"
info "Using appimagetool: ${APPIMAGETOOL}"

# ---------------------------------------------------------------------------
# Prepare AppDir
# ---------------------------------------------------------------------------
APPDIR="$(mktemp -d /tmp/Droidplane-AppDir-XXXXXX)"
trap 'rm -rf "${APPDIR}"' EXIT

info "Building AppDir at: ${APPDIR}"

# --- Copy distributable content (bin/ + lib/) ---
cp -a "${DIST_DIR}/bin" "${APPDIR}/bin"
cp -a "${DIST_DIR}/lib" "${APPDIR}/lib"

# --- AppRun script ---
cat > "${APPDIR}/AppRun" <<'APPRUN'
#!/usr/bin/env bash
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "${HERE}/bin/Droidplane" "$@"
APPRUN
chmod +x "${APPDIR}/AppRun"

# --- .desktop file ---
cat > "${APPDIR}/droidplane.desktop" <<DESKTOP
[Desktop Entry]
Name=Droidplane
Comment=MindManager viewer for Android/Linux
Exec=Droidplane
Icon=droidplane
Type=Application
Categories=Office;FlowChart;X-MindMapping;
Terminal=false
StartupWMClass=Droidplane
DESKTOP

# --- Icon ---
ICON_SRC="${DIST_DIR}/lib/Droidplane.png"
if [[ ! -f "${ICON_SRC}" ]]; then
  # Fallback: search recursively
  ICON_SRC="$(find "${DIST_DIR}/lib" -name "*.png" | head -n1 || true)"
fi
if [[ -n "${ICON_SRC}" && -f "${ICON_SRC}" ]]; then
  cp "${ICON_SRC}" "${APPDIR}/droidplane.png"
else
  warn "No icon found in distributable lib/. AppImage will have no icon."
fi

# --- .DirIcon symlink (required by AppImageKit) ---
if [[ -f "${APPDIR}/droidplane.png" ]]; then
  ln -sf "droidplane.png" "${APPDIR}/.DirIcon"
fi

info "AppDir structure:"
ls -la "${APPDIR}"

# ---------------------------------------------------------------------------
# Build AppImage
# ---------------------------------------------------------------------------
mkdir -p "${OUTPUT_DIR}"
OUTPUT_FILE="${OUTPUT_DIR}/Droidplane-${VERSION}-x86_64.AppImage"

info "Running appimagetool..."
APPIMAGE_EXTRACT_AND_RUN=1 "${APPIMAGETOOL}" \
  --appimage-extract-and-run \
  "${APPDIR}" \
  "${OUTPUT_FILE}" \
  2>&1 || error "appimagetool failed. See output above."

if [[ ! -f "${OUTPUT_FILE}" ]]; then
  error "appimagetool exited 0 but output file not found: ${OUTPUT_FILE}"
fi

chmod +x "${OUTPUT_FILE}"
ok "AppImage created: ${OUTPUT_FILE}"
info "Size: $(du -sh "${OUTPUT_FILE}" | cut -f1)"
