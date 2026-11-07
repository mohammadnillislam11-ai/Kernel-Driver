#!/bin/sh
# ==============================================================================
# NINMODS Kernel Security Bypass Engine
# ==============================================================================

if [ "$(id -u)" -ne 0 ]; then
    echo "[*] Elevating to root..."
    exec su -c "sh \"$0\" \"$@\""
fi

echo "[+] Initializing Kernel Security Bypass..."

# 1. Enforce permissive SELinux if required
if [ -f /sys/fs/selinux/enforce ]; then
    echo 0 > /sys/fs/selinux/enforce 2>/dev/null
    echo "[+] SELinux status set to permissive."
fi

# 2. Relax memory access controls
echo 0 > /proc/sys/kernel/yama/ptrace_scope 2>/dev/null
echo 0 > /proc/sys/kernel/kptr_restrict 2>/dev/null
echo 0 > /proc/sys/kernel/perf_event_paranoid 2>/dev/null

echo "[+] Memory protection fences unlocked."
echo "[+] Kernel Security Bypass successfully applied."
exit 0
