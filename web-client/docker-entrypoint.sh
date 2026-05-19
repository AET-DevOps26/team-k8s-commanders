#!/bin/sh
set -e

# Write runtime env vars into /config.js so the SPA can read window.__ENV__
cat > /usr/share/nginx/html/config.js <<EOF
window.__ENV__ = {
  PUBLIC_API_URL: "${PUBLIC_API_URL:-}"
};
EOF

exec nginx -g 'daemon off;'
