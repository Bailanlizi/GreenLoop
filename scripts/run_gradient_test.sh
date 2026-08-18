#!/bin/bash
# 梯度压测: 10/50/100/200 并发, 每级 1000 请求
set -e

JMETER="/d/App_download/apache-jmeter-5.6.3/bin/jmeter.sh"
JMX="D:/Project/greenloop_pay_test.jmx"
RESET_SQL="/d/Project/GreenLoop/scripts/reset_pay_test_data.sql"
# bash 用 unix 路径
OUT_DIR_UNIX="/d/Project/GreenLoop/data/jmeter_out/gradient"
# JMeter 用 Windows 路径
OUT_DIR_WIN="D:/Project/GreenLoop/data/jmeter_out/gradient"
mkdir -p "$OUT_DIR_UNIX"

run_level() {
  local threads=$1 rampup=$2 loops=$3 level=$4
  echo "===== Level: ${level} (threads=${threads}, ramp=${rampup}s, loops=${loops}) ====="
  echo "[$(date +%H:%M:%S)] Resetting data..."
  MYSQL_PWD=051012 mysql -uroot -h127.0.0.1 < "$RESET_SQL" 2>&1 | tail -2
  echo "[$(date +%H:%M:%S)] Running JMeter..."
  cd /d/App_download/apache-jmeter-5.6.3/bin
  ./jmeter.sh -n -t "$JMX" \
    -Jthreads=$threads -Jrampup=$rampup -Jloops=$loops \
    -l "${OUT_DIR_WIN}/pay_${level}.jtl" \
    -e -o "${OUT_DIR_WIN}/pay_${level}_report" \
    2>&1 | grep -vE "WARN|WARNING"
  echo "[$(date +%H:%M:%S)] Level ${level} done."
  echo ""
}

# 4 个梯度: threads x loops = 1000 每次
run_level 10  10 100 "10concurrent"
run_level 50  20  20 "50concurrent"
run_level 100 30  10 "100concurrent"
run_level 200 40   5 "200concurrent"

echo "===== All gradient tests complete ====="
