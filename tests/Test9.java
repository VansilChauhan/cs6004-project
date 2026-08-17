class Animal {
    static int staticSpeak() {
        return 1;
    }

    int speak() {
        return 1;
    }
}

class Dog extends  Animal {
    int speak() {
        return 2;
    }
}

public class Test9 {
    public static void main(String[] args) {
        Animal d = new Dog();
        long result = 0;
        for (int i = 0; i < 10000; i++) {
            result += d.speak();
            result += Animal.staticSpeak();
        }
        System.out.println(result);
    }
}
