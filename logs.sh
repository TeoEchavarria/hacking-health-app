#!/bin/bash
# =============================================================================
# Hacking Health App - Log Viewer (Multi-Device Support)
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
#   ./logs.sh -b           # Ver logs de AMBOS dispositivos (phone + emulator)
#   ./logs.sh -1           # Forzar primer dispositivo
#   ./logs.sh -2           # Forzar segundo dispositivo
# =============================================================================

set -e

APP_PACKAGE="com.samsung.android.health.sdk.sample.healthdiary"

# TAGs reales de la app (verificados en el código)
APP_TAGS="AuthViewModel:* AuthRepository:* OAuthRepository:* OAuthRedirectActivity:* OpenWearablesRepo:* HealthDiaryApp:* TokenManager:*"
OAUTH_TAGS="AuthViewModel:* OAuthRepository:* OAuthRedirectActivity:* GoogleSignatureVerifier:* SignInClient:* Finsky:*"
SYNC_TAGS="OpenWearablesRepo:* OpenWearablesSDK:* SyncManager:* HealthConnectManager:* SamsungHealthManager:*"
GMS_TAGS="GoogleSignatureVerifier:* SignInClient:* Finsky:* GoogleAuthUtil:* GmsClient:*"
HR_TAGS="HeartRate:* WatchReceiver:* SensorData:* HealthData:* WearableReceiver:* Protocol:*"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Device selection
SELECTED_DEVICE=""
BOTH_DEVICES=false

print_header() {
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  Hacking Health App - Log Viewer${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
}

get_devices() {
    adb devices | grep -E "device$|emulator" | awk '{print $1}'
}

select_device() {
    local devices=($(get_devices))
    local count=${#devices[@]}
    
    if [ $count -eq 0 ]; then
        echo -e "${RED}Error: No hay dispositivos conectados${NC}"
        adb devices
        exit 1
    fi
    
    if [ $count -eq 1 ]; then
        SELECTED_DEVICE="${devices[0]}"
        return
    fi
    
    # Multiple devices - let user choose
    echo -e "${YELLOW}Múltiples dispositivos detectados:${NC}"
    echo ""
    local i=1
    for device in "${devices[@]}"; do
        local model=$(adb -s "$device" shell getprop ro.product.model 2>/dev/null || echo "Unknown")
        local type="📱"
        [[ "$device" == emulator* ]] && type="💻"
        echo -e "  ${CYAN}[$i]${NC} $type $device - $model"
        ((i++))
    done
    echo -e "  ${CYAN}[b]${NC} 🔄 Ver AMBOS dispositivos"
    echo ""
    
    read -p "Selecciona dispositivo (1-$count, b=ambos): " choice
    
    if [[ "$choice" == "b" || "$choice" == "B" ]]; then
        BOTH_DEVICES=true
        return
    fi
    
    if [[ "$choice" =~ ^[0-9]+$ ]] && [ "$choice" -ge 1 ] && [ "$choice" -le $count ]; then
        SELECTED_DEVICE="${devices[$((choice-1))]}"
    else
        echo -e "${RED}Selección inválida${NC}"
        exit 1
    fi
}

check_adb() {
    if ! command -v adb &> /dev/null; then
        echo -e "${RED}Error: adb no encontrado. Instala Android SDK Platform Tools${NC}"
        exit 1
    fi
}

# Wrapper for adb commands with device selection
adb_cmd() {
    if [ -n "$SELECTED_DEVICE" ]; then
        adb -s "$SELECTED_DEVICE" "$@"
    else
        adb "$@"
    fi
}

get_app_pid() {
    adb_cmd shell pidof "$APP_PACKAGE" 2>/dev/null || echo ""
}

# View logs from TWO selected devices simultaneously
view_both_devices() {
    local devices=($(get_devices))
    local count=${#devices[@]}
    
    if [ $count -lt 2 ]; then
        echo -e "${RED}Se necesitan al menos 2 dispositivos para esta opción${NC}"
        exit 1
    fi
    
    echo -e "${CYAN}══════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  Selecciona los 2 dispositivos a monitorear${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════════${NC}"
    
    for i in "${!devices[@]}"; do
        local device="${devices[$i]}"
        local model=$(adb -s "$device" shell getprop ro.product.model 2>/dev/null || echo "Unknown")
        local type="📱"
        [[ "$device" == emulator* ]] && type="💻"
        echo -e "  [$(($i+1))] $type ${CYAN}$device${NC} - $model"
    done
    
    echo ""
    read -p "Primer dispositivo (1-$count): " dev1_choice
    read -p "Segundo dispositivo (1-$count): " dev2_choice
    
    # Validate selections
    if ! [[ "$dev1_choice" =~ ^[0-9]+$ ]] || [ "$dev1_choice" -lt 1 ] || [ "$dev1_choice" -gt $count ]; then
        echo -e "${RED}Selección inválida para primer dispositivo${NC}"
        exit 1
    fi
    if ! [[ "$dev2_choice" =~ ^[0-9]+$ ]] || [ "$dev2_choice" -lt 1 ] || [ "$dev2_choice" -gt $count ]; then
        echo -e "${RED}Selección inválida para segundo dispositivo${NC}"
        exit 1
    fi
    if [ "$dev1_choice" -eq "$dev2_choice" ]; then
        echo -e "${RED}Debes seleccionar dispositivos diferentes${NC}"
        exit 1
    fi
    
    local device1="${devices[$((dev1_choice-1))]}"
    local device2="${devices[$((dev2_choice-1))]}"
    local model1=$(adb -s "$device1" shell getprop ro.product.model 2>/dev/null || echo "Unknown")
    local model2=$(adb -s "$device2" shell getprop ro.product.model 2>/dev/null || echo "Unknown")
    
    echo ""
    echo -e "${YELLOW}Mostrando logs de:${NC}"
    echo -e "  ${CYAN}[1]${NC} $device1 - $model1"
    echo -e "  ${MAGENTA}[2]${NC} $device2 - $model2"
    echo -e "${YELLOW}Presiona Ctrl+C para salir...${NC}"
    echo ""
    
    # Cleanup on exit
    trap "kill 0 2>/dev/null" EXIT
    
    # Start logcat for both selected devices
    (adb -s "$device1" logcat | grep --line-buffered -iE "(healthdiary|HeartRate|WatchReceiver|SensorData|Wearable|OpenWearables|AUTH|SYNC|API)" | while read line; do
        echo -e "${CYAN}[1]${NC} $line"
    done) &
    
    (adb -s "$device2" logcat | grep --line-buffered -iE "(healthdiary|HeartRate|WatchReceiver|SensorData|Wearable|OpenWearables|AUTH|SYNC|API)" | while read line; do
        echo -e "${MAGENTA}[2]${NC} $line"
    done) &
    
    wait
}

# View heart rate and wearable data flow logs
view_hr_data_logs() {
    echo -e "${CYAN}══════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  Heart Rate & Wearable Data Flow Logs${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════════${NC}"
    echo -e "Monitoreando: HeartRate, WatchReceiver, SensorData, Wearable, Protocol"
    echo -e "${YELLOW}Presiona Ctrl+C para salir...${NC}"
    echo ""
    adb_cmd logcat | grep --line-buffered -iE "(HeartRate|heartRate|WatchReceiver|WearableReceiver|SensorData|sensor_data|Protocol|health/daily|health/hr|WearableListenerService|DataClient|MessageClient)"
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
    echo ""
    echo -e "${CYAN}Multi-dispositivo:${NC}"
    echo "  -b, --both   Ver logs de AMBOS dispositivos (phone + emulator)"
    echo "  -hr          Ver flujo de datos de frecuencia cardíaca"
    echo "  -1           Forzar primer dispositivo"
    echo "  -2           Forzar segundo dispositivo"
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
    echo -e "${YELLOW}Para debugear flujo Watch → Phone → API:${NC}"
    echo "  ./logs.sh -b                 # Ver logs de ambos dispositivos"
    echo "  ./logs.sh -hr                # Ver datos de frecuencia cardíaca"
    echo ""
    echo -e "${YELLOW}Para ver todo el flujo:${NC}"
    echo "  ./logs.sh -f                 # Ver OAuth + OW + API"
    echo ""
}

clear_logs() {
    echo -e "${YELLOW}Limpiando buffer de logs...${NC}"
    adb_cmd logcat -c
    echo -e "${GREEN}✓ Logs limpiados. Ahora puedes reproducir el problema.${NC}"
}

dump_logs() {
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local filename="logs_${timestamp}.txt"
    
    echo -e "${YELLOW}Guardando logs a ${filename}...${NC}"
    
    {
        echo "=== Hacking Health App Logs ==="
        echo "Timestamp: $(date)"
        echo "Device: $(adb_cmd shell getprop ro.product.model)"
        echo "Android: $(adb_cmd shell getprop ro.build.version.release)"
        echo ""
        echo "=== App Info ==="
        echo "Package: $APP_PACKAGE"
        echo "PID: $(get_app_pid)"
        echo ""
        echo "=== OAuth/Auth Logs ==="
        adb_cmd logcat -d | grep -iE "(AuthViewModel|OAuthRepository|GoogleAuth|Google.*Sign|ApiException|DEVELOPER_ERROR|SignatureVerifier)" | tail -200
        echo ""
        echo "=== Error Logs ==="
        adb_cmd logcat -d | grep -iE "(healthdiary|$APP_PACKAGE)" | grep -iE "(error|exception|fail|crash)" | tail -100
    } > "$filename"
    
    echo -e "${GREEN}✓ Logs guardados en: ${filename}${NC}"
    echo -e "  Líneas: $(wc -l < "$filename")"
}

view_all_logs() {
    local pid=$(get_app_pid)
    
    if [ -z "$pid" ]; then
        echo -e "${YELLOW}App no está corriendo. Mostrando logs recientes...${NC}"
        adb_cmd logcat -d | grep -i "healthdiary" | tail -100
    else
        echo -e "${GREEN}App PID: ${pid}${NC}"
        echo -e "${YELLOW}Mostrando logs en tiempo real (Ctrl+C para salir)...${NC}"
        echo ""
        adb_cmd logcat --pid="$pid"
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
    adb_cmd logcat | grep --line-buffered -iE "(AuthViewModel|OAuthRepository|OAuthRedirect|GoogleAuth|Google.*Sign|SignIn|ApiException|DEVELOPER_ERROR|SignatureVerifier|idToken|client.*id)"
}

view_sync_logs() {
    echo -e "${YELLOW}Mostrando logs de Sync/OpenWearables (Ctrl+C para salir)...${NC}"
    echo ""
    adb_cmd logcat | grep --line-buffered -iE "(OpenWearablesRepo|OpenWearablesSDK|SyncManager|HealthConnect|SamsungHealth)"
}

view_error_logs() {
    echo -e "${YELLOW}Mostrando errores y excepciones (Ctrl+C para salir)...${NC}"
    echo ""
    adb_cmd logcat "*:E" | grep --line-buffered -iE "(healthdiary|$APP_PACKAGE|Auth|OAuth|OpenWearables|exception|error|fail|crash)"
}

view_filtered_logs() {
    echo -e "${YELLOW}Mostrando logs filtrados de la app (Ctrl+C para salir)...${NC}"
    echo ""
    adb_cmd logcat | grep --line-buffered -iE "(AuthViewModel|AuthRepository|OAuthRepository|OpenWearablesRepo|HealthDiaryApp|TokenManager)"
}

view_gms_logs() {
    echo -e "${YELLOW}Mostrando logs de Google Play Services (Ctrl+C para salir)...${NC}"
    echo ""
    adb_cmd logcat | grep --line-buffered -iE "(GoogleSignatureVerifier|SignInClient|Finsky|GmsClient|package.*info|SHA)"
}

view_openwearables_logs() {
    echo -e "${CYAN}══════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  OpenWearables Credentials & SDK Logs${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════════${NC}"
    echo -e "Monitoreando: OpenWearablesRepo, TokenManager, credentials, open_wearables"
    echo -e "${YELLOW}Presiona Ctrl+C para salir...${NC}"
    echo ""
    adb_cmd logcat | grep --line-buffered -iE "(OpenWearables|TokenManager|open_wearables|ow_user|ow_access|owCreds|credentials.*stored|auto.*login|saveOpenWearables|hasOpenWearables)"
}

view_api_logs() {
    echo -e "${CYAN}══════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  API/Retrofit Request & Response Logs${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════════${NC}"
    echo -e "Monitoreando: API_REQUEST, API_RESPONSE, Retrofit, HTTP"
    echo -e "${YELLOW}Presiona Ctrl+C para salir...${NC}"
    echo ""
    adb_cmd logcat | grep --line-buffered -iE "(API_REQUEST|API_RESPONSE|API_ERROR|Retrofit|oauth/token|/login|authenticateWithOAuth|response.*code|status.*code|\{.*access_token|\{.*open_wearables)"
}

view_full_login_flow() {
    echo -e "${CYAN}══════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  Full Login Flow (OAuth + OpenWearables + API)${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════════${NC}"
    echo -e "Monitoreando todo el flujo de autenticación..."
    echo -e "${YELLOW}Presiona Ctrl+C para salir...${NC}"
    echo ""
    adb_cmd logcat | grep --line-buffered -iE "(AuthViewModel|OAuthRepository|OpenWearables|TokenManager|API_REQUEST|API_RESPONSE|API_ERROR|oauth/token|Google.*Sign|Backend.*auth|saveOpenWearables|hasOpenWearables|credentials|access_token|open_wearables|Network error|timeout|Success|Failed)"
}

# =============================================================================
# Main
# =============================================================================

print_header
check_adb

# Auto-select device if multiple connected (except for help or both-devices mode)
if [[ "${1:-}" != "-h" && "${1:-}" != "--help" && "${1:-}" != "-b" && "${1:-}" != "--both" ]]; then
    devices=($(get_devices))
    if [ ${#devices[@]} -gt 1 ]; then
        echo -e "${YELLOW}Múltiples dispositivos detectados${NC}"
        select_device
        # If user selected "both", run view_both_devices and exit
        if [ "$BOTH_DEVICES" = true ]; then
            view_both_devices
            exit 0
        fi
    elif [ ${#devices[@]} -eq 1 ]; then
        SELECTED_DEVICE="${devices[0]}"
    fi
fi

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
    -b|--both)
        view_both_devices
        ;;
    -hr)
        view_hr_data_logs
        ;;
    -1)
        SELECTED_DEVICE=1
        view_filtered_logs
        ;;
    -2)
        SELECTED_DEVICE=2
        view_filtered_logs
        ;;
    *)
        view_filtered_logs
        ;;
esac
