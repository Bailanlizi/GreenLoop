# -*- coding: utf-8 -*-
"""解析 JMeter JTL，输出每个接口的 TPS / 延迟分位 / 错误率"""
import csv, sys, statistics, collections

def analyze(path, label_filter=None):
    rows = list(csv.DictReader(open(path, encoding='utf-8')))
    groups = collections.defaultdict(list)
    for r in rows:
        if label_filter and label_filter not in r['label']:
            continue
        groups[r['label']].append(r)
    out = []
    for label, data in groups.items():
        ts = sorted(float(r['elapsed']) for r in data)
        err = sum(1 for r in data if r['success'] != 'true')
        n = len(data)
        # 按时间戳窗口计算 TPS（首尾时间差）
        stamps = sorted(float(r['timeStamp']) for r in data)
        duration_s = max((stamps[-1] - stamps[0]) / 1000.0, 0.001)
        tps = n / duration_s
        p50 = ts[int(n*0.50)-1] if n else 0
        p90 = ts[int(n*0.90)-1] if n else 0
        p99 = ts[int(n*0.99)-1] if n else 0
        out.append((label, n, err, err/n*100 if n else 0, tps,
                    statistics.mean(ts) if ts else 0, p50, p90, p99, max(ts) if ts else 0))
    return out

if __name__ == '__main__':
    path = sys.argv[1]
    print(f"{'接口':<20}{'样本':>7}{'错误':>6}{'错误率%':>9}{'TPS':>9}{'Avg(ms)':>9}{'P50':>7}{'P90':>7}{'P99':>7}{'Max':>7}")
    for label, n, err, errpct, tps, avg, p50, p90, p99, mx in analyze(path):
        print(f"{label:<20}{n:>7}{err:>6}{errpct:>9.2f}{tps:>9.1f}{avg:>9.1f}{p50:>7.0f}{p90:>7.0f}{p99:>7.0f}{mx:>7.0f}")
