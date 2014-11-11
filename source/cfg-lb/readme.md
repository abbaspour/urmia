building nginx

http://nginx.org/en/docs/http/ngx_http_auth_request_module.html
important module to enable: ngx_http_auth_request_module

./configure --prefix=/usr/local/Cellar/nginx/1.5.10 \
    --with-http_ssl_module --with-pcre --with-ipv6 \
    --sbin-path=/usr/local/Cellar/nginx/1.5.10/bin/nginx \
    --with-cc-opt=-I/usr/local/Cellar/pcre/8.34/include \
    --with-cc-opt=-I/usr/local/Cellar/openssl/1.0.1f/include \
    --with-ld-opt=-L/usr/local/Cellar/pcre/8.34/lib \
    --with-ld-opt=-L/usr/local/Cellar/openssl/1.0.1f/lib \
    --conf-path=/usr/local/etc/nginx/nginx.conf \
    --pid-path=/usr/local/var/run/nginx.pid \
    --lock-path=/usr/local/var/run/nginx.lock \
    --http-client-body-temp-path=/usr/local/var/run/nginx/client_body_temp \
    --http-proxy-temp-path=/usr/local/var/run/nginx/proxy_temp \
    --http-fastcgi-temp-path=/usr/local/var/run/nginx/fastcgi_temp \
    --http-uwsgi-temp-path=/usr/local/var/run/nginx/uwsgi_temp \
    --http-scgi-temp-path=/usr/local/var/run/nginx/scgi_temp \
    --http-log-path=/usr/local/var/log/nginx/access.log \
    --error-log-path=/usr/local/var/log/nginx/error.log \
    --with-http_gzip_static_module \
    --with-http_auth_request_module

