import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        // 读取用户输入的字符串
        String input = scanner.nextLine();
        scanner.close();
        
        // 调用反转方法并输出结果
        String reversed = reverseString(input);
        System.out.println(reversed);
    }
    
    // 反转字符串的方法
    public static String reverseString(String str) {
        return new StringBuilder(str).reverse().toString();
    }
}