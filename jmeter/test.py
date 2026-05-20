import requests
import threading
import time
import statistics
from datetime import datetime

# 测试配置
BASE_URL = "http://gateway:8080"
NUM_THREADS = 100
NUM_REQUESTS_PER_THREAD = 1
OUTPUT_FILE = "/results/test_report.txt"

# 结果收集
response_times = []
success_count = 0
failure_count = 0
lock = threading.Lock()

def get_token():
    """获取认证 token"""
    login_url = f"{BASE_URL}/api/auth/login"
    data = {
        "username": "admin",
        "password": "StructExam123"
    }
    try:
        response = requests.post(login_url, json=data, timeout=10)
        if response.status_code == 200:
            result = response.json()
            return result.get("data", {}).get("token")
    except Exception as e:
        print(f"登录失败: {e}")
    return None

def send_request(user_id, token):
    global success_count, failure_count
    
    headers = {
        "Content-Type": "application/json",
        "X-User-Id": str(user_id),
        "Authorization": f"Bearer {token}"
    }
    
    data = {
        "examId": 1,
        "questionId": 1,
        "code": "public class Test { public static void main(String[] args) {} }",
        "language": "JAVA"
    }
    
    submit_url = f"{BASE_URL}/api/code/submit"
    
    for _ in range(NUM_REQUESTS_PER_THREAD):
        start_time = time.time()
        try:
            response = requests.post(submit_url, headers=headers, json=data, timeout=30)
            elapsed = (time.time() - start_time) * 1000  # 转换为毫秒
            
            with lock:
                response_times.append(elapsed)
                if response.status_code == 200:
                    success_count += 1
                else:
                    failure_count += 1
                    print(f"用户 {user_id} 请求失败: {response.status_code}")
                    
        except Exception as e:
            elapsed = (time.time() - start_time) * 1000
            with lock:
                response_times.append(elapsed)
                failure_count += 1
                print(f"用户 {user_id} 请求异常: {e}")

def generate_report(total_time):
    """生成测试报告"""
    success_rate = success_count / (success_count + failure_count) * 100 if (success_count + failure_count) > 0 else 0
    tps = len(response_times) / (total_time / 1000) if total_time > 0 else 0
    
    report = []
    report.append("=" * 60)
    report.append("          代码提交高并发测试报告")
    report.append("=" * 60)
    report.append(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    report.append("")
    report.append("【测试配置】")
    report.append(f"  并发用户数: {NUM_THREADS}")
    report.append(f"  每个用户请求数: {NUM_REQUESTS_PER_THREAD}")
    report.append(f"  总请求数: {NUM_THREADS * NUM_REQUESTS_PER_THREAD}")
    report.append(f"  目标接口: {BASE_URL}/api/code/submit")
    report.append("")
    report.append("【测试结果】")
    report.append(f"  总耗时: {total_time:.2f} ms")
    report.append(f"  成功请求: {success_count}")
    report.append(f"  失败请求: {failure_count}")
    report.append(f"  成功率: {success_rate:.2f}%")
    report.append("")
    report.append("【响应时间统计】")
    if response_times:
        report.append(f"  最小: {min(response_times):.2f} ms")
        report.append(f"  最大: {max(response_times):.2f} ms")
        report.append(f"  平均: {statistics.mean(response_times):.2f} ms")
        report.append(f"  P95: {sorted(response_times)[int(len(response_times)*0.95)]:.2f} ms")
        report.append(f"  P99: {sorted(response_times)[int(len(response_times)*0.99)]:.2f} ms")
        report.append(f"  TPS: {tps:.2f}")
    else:
        report.append("  无有效响应数据")
    report.append("")
    report.append("=" * 60)
    
    # 输出到控制台
    print("\n" + "\n".join(report))
    
    # 写入文件
    try:
        with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
            f.write("\n".join(report))
        print(f"\n测试报告已保存到: {OUTPUT_FILE}")
    except Exception as e:
        print(f"保存报告失败: {e}")

def main():
    print(f"=== 代码提交高并发测试 ===")
    
    # 先登录获取 token
    print("正在登录获取认证 token...")
    token = get_token()
    if not token:
        print("登录失败，无法继续测试")
        return
    print("登录成功")
    
    print(f"\n并发用户数: {NUM_THREADS}")
    print(f"每个用户请求数: {NUM_REQUESTS_PER_THREAD}")
    print(f"总请求数: {NUM_THREADS * NUM_REQUESTS_PER_THREAD}")
    print(f"目标接口: {BASE_URL}/api/code/submit")
    print()
    
    # 创建线程
    threads = []
    for i in range(NUM_THREADS):
        t = threading.Thread(target=send_request, args=(i + 1, token))
        threads.append(t)
    
    # 启动线程
    start_time = time.time()
    for t in threads:
        t.start()
    
    # 等待所有线程完成
    for t in threads:
        t.join()
    
    total_time = (time.time() - start_time) * 1000
    
    # 生成报告
    generate_report(total_time)

if __name__ == "__main__":
    main()
