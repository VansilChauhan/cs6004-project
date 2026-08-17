class Animal {
    int speak() {
        return 1;
    }
}

class Dog extends Animal {
    int speak() {
        return 2;
    }
}

public class Test6 {
    public static void main(String[] args) {
        Animal d1 = new Dog();
        Animal d2 = new Dog();
        long result = 0;
        for (int i = 0; i < 10000; i++) {
            result += d1.speak();
            result += d2.speak();
        }
        System.out.println(result);
    }
}
