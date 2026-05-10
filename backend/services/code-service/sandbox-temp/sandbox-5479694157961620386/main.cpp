#include <iostream>
#include <string>
using namespace std;

int main() {
    string s;
    getline(cin, s);
    
    int left = 0;
    int right = s.size() - 1;
    
    // 首尾交换实现反转
    while (left < right) {
        char temp = s[left];
        s[left] = s[right];
        s[right] = temp;
        left++;
        right--;
    }
    
    cout << s << endl;
    return 0;
}