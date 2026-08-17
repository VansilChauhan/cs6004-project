package devirtualizer.PTAToolkit;

public class NullObject extends AbstractObject {
    private static final NullObject instance = new NullObject();

    private NullObject() {}

    public static NullObject getInstance() { 
        return instance;
    }

    public String toString() {
        return "NULL";
    }
}
