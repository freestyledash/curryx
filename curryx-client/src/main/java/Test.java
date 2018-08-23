/**
 * @author zhangyanqi
 * @since 1.0 2018/8/7
 */
public class Test extends Thread {

    static void dosth() {
        throw new RuntimeException("error");
    }

    public static void main(String[] args) {
        String a = "a" + "b";
        String b = "ab";
        String c = new String("ab");
        String d = new String("ab");
        System.out.println(a == b);
        System.out.println(a == c);
        System.out.println(c == d);
    }
}
