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

class Cat extends Animal {
    int speak() {
        return 3;
    }
}

public class Test10 {
    public static void main(String[] args) {
        Animal d = new Dog();
        Animal a = new Animal();
        long result = 0;
        for (int i = 0; i < 10000; i++) {
            result += d.speak(); // monomorphized
            result += a.speak(); // not monomorphized
        }
        System.out.println(result);
    }
}
