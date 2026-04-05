#!/bin/bash
# =============================================================================
# Hacking Health App - Log Viewer
# =============================================================================
# Usage:
#   ./logs.sh              # Ver logs en tiempo real (filtrados)
#   ./logs.sh -a           # Ver TODOS los logs de la app
#   ./logs.sh -o           # Ver solo logs de OAuth/Google Sign-In
#   ./logs.sh -s           # Ver solo logs de Sync/OpenWearables
#   ./logs.sh -w           # Ver logs de OpenWearables credentials
#   ./logs.sh -e           # Ver solo errores
#   ./logs.sh -c           # Limpiar logs y empezar fresh
#   ./logs.sh -d           # Dump: guardar logs a archivo
#   ./logs.sh -g           # Ver logs de Google Play Services
#   ./logs.sh -r           # Ver respuestas de API (Retrofit)
# =============================================================================

set -e

APP_PACKAGE="com.samsung.android.health.sdk.sample.healthdiary"

# TAGs reales de la app (verificados en el código)
APP_TAGS="AuthViewModel:* AuthRepository:* OAuthRepository:* OAuthRedirectActivity:* OpenWearablesRepo:* HealthDiaryApp:* TokenManager:*"
OAUTH_TAGS="AuthViewModel:* OAuthRepository:* OAuthRedirectActivity:* GoogleSignatureVerifier:* SignInClient:* Finsky:*"
SYNC_TAGS="OpenWearablesRepo:* OpenWearablesSDK:* SyncManager:* HealthConnectManager:* SamsungHealthManager:*"
GMS_TAGS="GoogleSignatureVerifier:* SignInClient:* Finsky:* GoogleAuthUtil:* GmsClient:*"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

print_header() {
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  Hacking Health App - Log Viewer${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
}

check_adb() {
    if ! command -v adb &> /dev/null; then
        echo -e "${RED}Error: adb no encontrado. Instala Android SDK Platform Tools${NC}"
        exit 1
    fi
    
    # Check device connected
    if ! adb devices | grep -q "device$"; then
        echo -e "${RED}Error: No hay dispositivo conectado${NC}"
        echo "Dispositivos disponibles:"
        adb devices
        exit 1
    fi
}

get_app_pid() {
    adb shell pidof "$APP_PACKAGE" 2>/dev/null || echo ""
}

show_help() {
    print_header
    echo ""
    echo "Uso: ./logs.sh [OPCIÓN]"
    echo ""
    echo "Opciones:"
    echo "  (sin args)   Ver logs de la app en tiempo real"
    echo "  -a, --all    Ver TODOS los logs de la app (por PID)"
    echo "  -o, --oauth  Ver logs de OAuth/Google Sign-In (AuthViewModel, OAuthRepository)"
    echo "  -w, --ow     Ver logs de OpenWearables credentials y SDK"
    echo "  -r, --api    Ver logs de API/Retrofit (requests y responses)"
    echo "  -g, --gms    Ver logs de Google Play Services (SignatureVerifier)"
    echo "  -s, --sync   Ver logs de Sync/OpenWearables"
    echo "  -e, --error  Ver solo errores y excepciones"
    echo "  -c, --clear  Limpiar buffer de logs"
    echo "  -d, --dump   Guardar logs a archivo"
    echo "  -f, --full   Ver TODO el flujo de login (OAuth + OW + API)"
    echo "  -h, --help   Mostrar esta ayuda"
    echo ""
    echo -e "${YELLOW}Para debugear Google Sign-In:${NC}"
    echo "  1. ./logs.sh -c              # Limpiar logs"
    echo "  2. Intenta login en la app"
    echo "  3. ./logs.sh -o              # Ver qué pasó"
    echo ""
    echo -e "${YELLOW}Para debugear OpenWearables:${NC}"
    echo "  1. ./logs.sh -c              # Limpiar logs"
    echo "  2. Intenta login en la app"
    echo "  3. ./logs.sh -w              # Ver credenciales OW"
    echo ""
    echo -e "${YELLOW}Para ver todo el flujo:${NC}"
    echo "  ./logs.sh -f                 # Ver OAuth + OW + API"
    echo ""
}

clear_logs() {
    echo -e "${YELLOW}Limpiando buffer de logs...${NC}"
    adb logcat -c
    echo -e "${GREEN}✓ Logs limpiados. Ahora puedes reproducir el problema.${NC}"
}

dump_logs() {
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local filename="logs_${timestamp}.txt"
    
    echo -e "${YELLOW}Guardando logs a ${filename}...${NC}"
    
    {
        echo "=== Hacking Health App Logs ==="
        echo "Timestamp: $(date)"
        echo "Device: $(adb shell getprop ro.product.model)"
        echo "Android: $(adb shell getprop ro.build.version.release)"
        echo ""
        echo "=== App Info ==="
        echo "Package: $APP_PACKAGE"
        echo "PID: $(get_app_pid)"
        echo ""
        echo "=== OAuth/Auth Logs ==="
        adb logcat -d | grep -iE "(AuthViewModel|OAuthRepository|GoogleAuth|Google.*Sign|ApiException|DEVELOPER_ERROR|SignatureVerifier)" | tail -200
        echo ""
        echo "=== Error Logs ==="
        adb logcat -d | grep -iE "(healthdiary|$APP_PACKAGE)" | grep -iE "(error|exception|fail|crash)" | tail -100
    } > "$filename"
    
    echo -e "${GREEN}✓ Logs guardados en: ${filename}${NC}"
    echo -e "  Líneas: $(wc -l < "$filename")"
}

view_all_logs() {
    local pid=$(get_app_pid)
    
    if [ -z "$pid" ]; then
        echo -e "${YELLOW}App no está corriendo. Mostrando logs recientes...${NC}"
        adb logcat -d | grep -i "healthdiary" | tail -100
    else
        echo -e "${GREEN}App PID: ${pid}${NC}"
        echo -e "${YELLOW}Mostrando logs en tiempo real (Ctrl+C para salir)...${NC}"
        echo ""
        adb logcat --pid="$pid"
    fi
}

view_oauth_logs() {
    echo -e "${YELLOW}══════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  OAuth/Google Sign-In Logs${NC}"
    echo -e "${YELLOW}══════════════════════════════════════════════════════════════${NC}"
    echo -e "TAGs monitoreados: AuthViewModel, OAuthRepository, GoogleSignatureVerifier"
    echo -e "${YELLOW}Presiona Ctrl+C para salir...${NC}"
    echo ""
    # Usar grep en tiempo real con los patrones correctos
    adb logcat | grep --line-buffered -iE "(AuthViewModel|OAuthRepository|OAuthRedirect|GoogleAuth|Google.*Sign|SignIn|ApiException|DEVELOPER_ERROR|SignatureVerifier|idToken|client.*id)"
}

view_sync_logs() {
    echo -e "${YELLOW}Mostrando logs de Sync/OpenWearables (Ctrl+C para salir)...${NC}"
    echo ""
    adb logcat | grep --line-buffered -iE "(OpenWearablesRepo|OpenWearablesSDK|SyncManager|HealthConnect|SamsungHealth)"
}

view_error_logs() {
    echo -e "${YELLOW}Mostrando errores y excepciones (Ctrl+C para salir)...${NC}"
    echo ""
    adb logcat "*:E" | grep --line-buffered -iE "(healthdiary|$APP_PACKAGE|Auth|OAuth|OpenWearables|exception|error|fail|crash)"
}

view_filtered_logs() {
    echo -e "${YELLOW}Mostrando logs filtrados de la app (Ctrl+C para salir)...${NC}"
    echo ""
    adb logcat | grep --line-buffered -iE "(AuthViewModel|AuthRepository|OAuthRepository|OpenWearablesRepo|HealthDiaryApp|TokenManager)"
}

view_gms_logs() {
    echo -e "${YELLOW}Mostrando logs de Google Play Services (Ctrl+C para salir)...${NC}"
    echo ""
    adb logcat | grep --line-buffered -iE "(GoogleSignatureVerifier|SignInClient|Finsky|GmsClient|package.*info|SHA)"
}

view_openwearables_logs() {
    echo -e "${CYAN}══════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  OpenWearables Credentials & SDK Logs${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════════${NC}"
    echo -e "Monitoreando: OpenWearablesRepo, TokenManager, credentials, open_wearables"
    echo -e "${YELLOW}Presiona Ctrl+C para salir...${NC}"
    echo ""
    adb logcat | grep --line-buffered -iE "(OpenWearables|TokenManager|open_wearables|ow_user|ow_access|owCreds|credentials.*stored|auto.*login|saveOpenWearables|hasOpenWearables)"
}

view_api_logs() {
    echo -e "${CYAN}══════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  API/Retrofit Request & Response Logs${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════════${NC}"
    echo -e "Monitoreando: API_REQUEST, API_RESPONSE, Retrofit, HTTP"
    echo -e "${YELLOW}Presiona Ctrl+C para salir...${NC}"
    echo ""
    adb logcat | grep --line-buffered -iE "(API_REQUEST|API_RESPONSE|API_ERROR|Retrofit|oauth/token|/login|authenticateWithOAuth|response.*code|status.*code|\{.*access_token|\{.*open_wearables)"
}

view_full_login_flow() {
    echo -e "${CYAN}══════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  Full Login Flow (OAuth + OpenWearables + API)${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════════${NC}"
    echo -e "Monitoreando todo el flujo de autenticación..."
    echo -e "${YELLOW}Presiona Ctrl+C para salir...${NC}"
    echo ""
    adb logcat | grep --line-buffered -iE "(AuthViewModel|OAuthRepository|OpenWearables|TokenManager|API_REQUEST|API_RESPONSE|API_ERROR|oauth/token|Google.*Sign|Backend.*auth|saveOpenWearables|hasOpenWearables|credentials|access_token|open_wearables|Network error|timeout|Success|Failed)"
}

# =============================================================================
# Main
# =============================================================================

print_header
check_adb

case "${1:-}" in
    -h|--help)
        show_help
        ;;
    -c|--clear)
        clear_logs
        ;;
    -d|--dump)
        dump_logs
        ;;
    -a|--all)
        view_all_logs
        ;;
    -o|--oauth)
        view_oauth_logs
        ;;
    -g|--gms)
        view_gms_logs
        ;;
    -w|--ow)
        view_openwearables_logs
        ;;
    -r|--api)
        view_api_logs
        ;;
    -f|--full)
        view_full_login_flow
        ;;
    -s|--sync)
        view_sync_logs
        ;;
    -e|--error)
        view_error_logs
        ;;
    *)
        view_filtered_logs
        ;;
esac
