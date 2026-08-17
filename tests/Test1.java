
class A {
    int foo() {
        return 1;
    }
}

class B extends A {
    int foo() {
        return 2;
    }
}

class C extends B {
    int foo() {
        return 3;
    }
}

public class Test1 {
    public static void main(String[] args) {
        A obj = new C();
        long result = 0;
        for (int i = 0; i < 10000; i++) {
            result += obj.foo();
        }
        System.out.println(result);
    }
}
