#!/bin/sh
# Установка WControl CGI на OpenWrt.
# Запускать на роутере из каталога с файлами проекта.
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CGI_DIR=/www/cgi-bin

mkdir -p "$CGI_DIR"
cp "$SCRIPT_DIR/proxy" "$CGI_DIR/proxy"
cp "$SCRIPT_DIR/proxy_on.sh" /etc/proxy_on.sh
cp "$SCRIPT_DIR/proxy_off.sh" /etc/proxy_off.sh
chmod 755 "$CGI_DIR/proxy"
chmod 700 /etc/proxy_on.sh /etc/proxy_off.sh

if [ ! -f /etc/proxy_token ]; then
    umask 077
    if command -v openssl >/dev/null 2>&1; then
        openssl rand -hex 32 > /etc/proxy_token
    else
        printf '%s\n' 'CHANGE-ME-WITH-A-LONG-RANDOM-TOKEN' > /etc/proxy_token
    fi
    chmod 600 /etc/proxy_token
    echo "Создан /etc/proxy_token. Покажите его командой: cat /etc/proxy_token"
else
    echo "Существующий /etc/proxy_token сохранён."
fi

printf '%s\n' off > /tmp/wcontrol_proxy_state
chmod 600 /tmp/wcontrol_proxy_state
/etc/init.d/uhttpd restart

echo "WControl CGI установлен. Проверьте токен и настройте /etc/proxy_on.sh и /etc/proxy_off.sh."
