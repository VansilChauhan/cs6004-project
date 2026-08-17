class Animal {
    int speak(int volume) {
        return volume;
    }
}

class Dog extends Animal {
    int speak(int volume) {
        return volume * 2;
    }
}

public class Test5 {
    public static void main(String[] args) {
        Animal d = new Dog();
        long result = 0;
        for (int i = 0; i < 10000; i++) {
            result += d.speak(5);
        }
        System.out.println(result);
    }
}
