# -*- coding: utf-8 -*-
"""
批量登录 1000 用户，生成 token CSV 供纯支付压测使用。
输出: data/jmeter_pay_token_only.csv
格式: username,orderId,token
"""
import csv
import json
import urllib.request
import urllib.error
import sys
import time

BASE_URL = "http://localhost:8080"
INPUT_CSV = "D:/Project/GreenLoop/data/jmeter_pay_test_1order_per_user.csv"
OUTPUT_CSV = "D:/Project/GreenLoop/data/jmeter_pay_token_only.csv"
BATCH_SIZE = 50  # 并发登录批次大小

def login(username, password):
    """调用登录接口获取 token"""
    url = f"{BASE_URL}/users/authenticate"
    body = json.dumps({"username": username, "password": password}).encode("utf-8")
    req = urllib.request.Request(url, data=body, headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            return data.get("data", {}).get("token", "")
    except Exception as e:
        return ""

def main():
    # 读取输入 CSV
    rows = []
    with open(INPUT_CSV, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for r in reader:
            rows.append(r)

    print(f"共 {len(rows)} 个用户待登录")
    success = 0
    fail = 0
    results = []

    for i, row in enumerate(rows):
        token = login(row["username"], row["password"])
        if token and token != "NOT_FOUND":
            success += 1
            results.append({
                "username": row["username"],
                "orderId": row["orderId"],
                "token": token,
            })
        else:
            fail += 1
            print(f"  登录失败: {row['username']}")

        if (i + 1) % 100 == 0:
            print(f"  进度: {i+1}/{len(rows)} (成功 {success}, 失败 {fail})")

    # 写入输出 CSV
    with open(OUTPUT_CSV, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=["username", "orderId", "token"])
        writer.writeheader()
        writer.writerows(results)

    print(f"\n完成: 成功 {success}, 失败 {fail}")
    print(f"输出: {OUTPUT_CSV}")

if __name__ == "__main__":
    main()
