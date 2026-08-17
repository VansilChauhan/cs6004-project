interface Speakable {
    int speak();
}

class Dog implements Speakable {
    public int speak() {
        return 2;
    }
}

public class Test2 {
    public static void main(String[] args) {
        Speakable d = new Dog();
        long result = 0;
        for (int i = 0; i < 10000; i++) {
            result += d.speak();
        }
        System.out.println(result);
    }
}
