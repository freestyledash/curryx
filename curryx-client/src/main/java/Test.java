import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

/**
 * @author zhangyanqi
 * @since 1.0 2018/8/7
 */
public class Test {


    interface T {

    }

    public static void main(String[] args) {

        T o = (T) Proxy.newProxyInstance(Test.class.getClassLoader(), new Class[]{T.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return  proxy.toString();
            }
        });

        o.hashCode();

    }
}
