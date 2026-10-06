#!/bin/sh
# Заглушка включения проксирования.
# Раскомментируйте и адаптируйте только один нужный вариант.

# Passwall:
# /etc/init.d/passwall enable
# /etc/init.d/passwall restart

# OpenClash:
# /etc/init.d/openclash enable
# /etc/init.d/openclash restart

# Sing-box:
# /etc/init.d/sing-box enable
# /etc/init.d/sing-box restart

# Xray:
# /etc/init.d/xray enable
# /etc/init.d/xray restart

# Пример включения собственного nftables-набора:
# nft -f /etc/nftables.d/proxy.nft

exit 0