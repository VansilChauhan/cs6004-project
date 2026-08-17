class Animal {
    int speak() {
        return 1;
    }

    int eat() {
        return 1;
    }

    int sleep() {
        return 1;
    }

    int run() {
        return 1;
    }
}

class Dog extends Animal {
    int speak() {
        return 2;
    }

    int eat() {
        return 2;
    }

    int sleep() {
        return 2;
    }

    int run() {
        return 2;
    }
}

public class Test3 {
    public static void main(String[] args) {
        Animal d = new Dog();
        long result = 0;
        for (int i = 0; i < 10000; i++) {
            result += d.speak();
            result += d.eat();
            result += d.sleep();
            result += d.run();
        }
        System.out.println(result);
    }
}
