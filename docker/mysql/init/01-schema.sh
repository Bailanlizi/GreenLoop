#!/bin/sh
set -eu

mysql --protocol=socket -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 < /schema/campus_trade.sql
