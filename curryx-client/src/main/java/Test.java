/**
 * @author zhangyanqi
 * @since 1.0 2018/8/7
 */
public class Test extends Thread {

    static class A {
    }

    static class B extends A {

    }

    public static void main(String[] args) {
        boolean assignableFrom = A.class.isAssignableFrom(A.class);
        System.out.println(assignableFrom);
    }
}
