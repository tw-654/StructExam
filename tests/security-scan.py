#!/usr/bin/env python3
"""
StructExam 安全扫描脚本
用于检测常见安全漏洞：SQL注入、XSS攻击、敏感信息泄露等
"""

import requests
import json
import argparse
from urllib.parse import urljoin

# SQL注入测试payloads
SQL_INJECTION_PAYLOADS = [
    "' OR '1'='1",
    "' OR 1=1--",
    "' UNION SELECT 1,2,3--",
    "' AND SLEEP(5)--",
    "admin' --",
    "' OR EXISTS(SELECT * FROM users)--",
]

# XSS测试payloads
XSS_PAYLOADS = [
    "<script>alert('XSS')</script>",
    "<img src=x onerror=alert(1)>",
    "<svg onload=alert('XSS')>",
    "';alert('XSS');//",
    "<body onload=alert(1)>",
]

# 敏感信息泄露测试路径
SENSITIVE_PATHS = [
    "/api/users",
    "/api/admin/users",
    "/api/exam/all",
    "/config",
    "/env",
    "/actuator",
    "/actuator/health",
    "/actuator/env",
    "/actuator/beans",
    "/swagger-ui.html",
    "/v2/api-docs",
    "/v3/api-docs",
]

def test_sql_injection(base_url, endpoint, params):
    """测试SQL注入漏洞"""
    vulnerabilities = []
    url = urljoin(base_url, endpoint)
    
    for payload in SQL_INJECTION_PAYLOADS:
        test_params = {k: payload for k in params}
        try:
            response = requests.get(url, params=test_params, timeout=10)
            # 检查是否有SQL错误信息
            sql_errors = ["SQL syntax", "MySQL syntax", "PostgreSQL", "ORA-", "SQLite"]
            for error in sql_errors:
                if error.lower() in response.text.lower():
                    vulnerabilities.append({
                        "type": "SQL注入",
                        "payload": payload,
                        "endpoint": endpoint,
                        "evidence": f"响应中包含SQL错误: {error}"
                    })
                    break
        except Exception as e:
            pass
    
    return vulnerabilities

def test_xss(base_url, endpoint, params):
    """测试XSS漏洞"""
    vulnerabilities = []
    url = urljoin(base_url, endpoint)
    
    for payload in XSS_PAYLOADS:
        test_params = {k: payload for k in params}
        try:
            response = requests.get(url, params=test_params, timeout=10)
            # 检查payload是否在响应中未被过滤
            if payload in response.text:
                vulnerabilities.append({
                    "type": "XSS攻击",
                    "payload": payload,
                    "endpoint": endpoint,
                    "evidence": "Payload未被过滤直接返回"
                })
        except Exception as e:
            pass
    
    return vulnerabilities

def test_sensitive_info_leak(base_url):
    """测试敏感信息泄露"""
    vulnerabilities = []
    
    for path in SENSITIVE_PATHS:
        url = urljoin(base_url, path)
        try:
            response = requests.get(url, timeout=10)
            if response.status_code == 200:
                # 检查是否包含敏感信息
                sensitive_keywords = ["password", "secret", "token", "jwt", "credential"]
                content = response.text.lower()
                found_keywords = [kw for kw in sensitive_keywords if kw in content]
                if found_keywords:
                    vulnerabilities.append({
                        "type": "敏感信息泄露",
                        "endpoint": path,
                        "evidence": f"发现敏感关键词: {', '.join(found_keywords)}"
                    })
                elif "application/json" in response.headers.get("Content-Type", ""):
                    # JSON响应可能包含敏感配置
                    vulnerabilities.append({
                        "type": "敏感信息泄露风险",
                        "endpoint": path,
                        "evidence": "公开暴露JSON接口，可能包含敏感配置"
                    })
        except Exception as e:
            pass
    
    return vulnerabilities

def run_security_scan(base_url):
    """执行完整安全扫描"""
    print(f"========== 安全扫描开始 ==========")
    print(f"扫描目标: {base_url}")
    print("")
    
    all_vulnerabilities = []
    
    # 1. SQL注入测试
    print("[1/3] 测试SQL注入漏洞...")
    sql_endpoints = [
        ("/api/auth/login", ["username", "password"]),
        ("/api/exam/list", ["keyword"]),
        ("/api/exam/{id}", ["id"]),
        ("/api/code/{exam}/{question}", ["exam", "question"]),
    ]
    for endpoint, params in sql_endpoints:
        vulns = test_sql_injection(base_url, endpoint, params)
        all_vulnerabilities.extend(vulns)
    
    # 2. XSS测试
    print("[2/3] 测试XSS漏洞...")
    xss_endpoints = [
        ("/api/exam/list", ["keyword"]),
        ("/api/code/submit", ["code"]),
    ]
    for endpoint, params in xss_endpoints:
        vulns = test_xss(base_url, endpoint, params)
        all_vulnerabilities.extend(vulns)
    
    # 3. 敏感信息泄露测试
    print("[3/3] 测试敏感信息泄露...")
    vulns = test_sensitive_info_leak(base_url)
    all_vulnerabilities.extend(vulns)
    
    # 输出报告
    print("")
    print("========== 安全扫描报告 ==========")
    if all_vulnerabilities:
        print(f"发现 {len(all_vulnerabilities)} 个安全问题:")
        for i, vuln in enumerate(all_vulnerabilities, 1):
            print(f"\n{i}. [{vuln['type']}] {vuln['endpoint']}")
            print(f"   Payload: {vuln.get('payload', 'N/A')}")
            print(f"   证据: {vuln['evidence']}")
    else:
        print("未发现安全漏洞")
    
    print("")
    print("========== 扫描完成 ==========")
    
    # 保存报告
    with open("security-scan-report.json", "w", encoding="utf-8") as f:
        json.dump(all_vulnerabilities, f, ensure_ascii=False, indent=2)
    
    print(f"报告已保存到: security-scan-report.json")
    return all_vulnerabilities

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="StructExam 安全扫描工具")
    parser.add_argument("--url", default="http://localhost:8080", help="目标URL")
    args = parser.parse_args()
    
    run_security_scan(args.url)