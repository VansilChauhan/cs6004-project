class Animal {
    int speak() {
        return 0;
    }
}

class Dog extends Animal {
    int speak() {
        return 0;
    }
}

public class Test8 {
    public static void main(String[] args) {
        Animal d = new Dog();
        long result = 0;
        for (int i = 0; i < 10000; i++) {
            result += d.speak();
        }
        System.out.println(result);
    }
}
