public class Main {
    public static void main(String[] args) {
        #include <iostream>
#include <string>
#include <algorithm>  // 用于 reverse 函数
using namespace std;

int main() {
    string s;
    getline(cin, s);  // 读取整行输入（支持空格）
    
    // 反转字符串
    reverse(s.begin(), s.end());
    
    // 输出结果
    cout << s << endl;
    return 0;
}
    }
}
