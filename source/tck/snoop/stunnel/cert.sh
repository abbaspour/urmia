#!/bin/sh
openssl req -new -x509 -days 365 -nodes -out stunnel.pem -keyout stunnel.pem
