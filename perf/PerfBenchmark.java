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

public class PerfBenchmark {
    public static void main(String[] args) {
        Animal d = new Dog();
        long result = 0;
        int iterations = 200_000_000;
        for (int i = 0; i < iterations; i++) {
            result += d.speak();
        }
        System.out.println(result);
    }
}
