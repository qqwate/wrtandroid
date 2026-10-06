#!/bin/sh
# Заглушка выключения проксирования.
# Раскомментируйте и адаптируйте только один нужный вариант.

# Passwall:
# /etc/init.d/passwall stop

# OpenClash:
# /etc/init.d/openclash stop

# Sing-box:
# /etc/init.d/sing-box stop

# Xray:
# /etc/init.d/xray stop

# Пример очистки собственного nftables-набора:
# nft delete table inet proxy

exit 0