class Dog {
    int speak() {
        return 2;
    }
}

public class Test4 {
    public static void main(String[] args) {
        Dog d = new Dog();
        long result = 0;
        for (int i = 0; i < 10000; i++) {
            result += d.speak();
        }
        System.out.println(result);
    }
}
