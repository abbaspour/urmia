
src     := src/main/nginx
config  := $(src)/nginx.conf
nginx   := /usr/local/Cellar/nginx/1.5.10/bin/nginx/nginx

all: test

logs:
	mkdir logs

conf: $(config) logs

test: conf
	@$(nginx) -p . -c $(config) -t

run: test
	$(nginx) -p . -c $(config)

clean:
	@rm -rf logs/*