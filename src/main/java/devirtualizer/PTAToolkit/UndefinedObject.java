package devirtualizer.PTAToolkit;

public class UndefinedObject extends AbstractObject {
    private static final UndefinedObject instance = new UndefinedObject();

    private UndefinedObject() {}

    public static UndefinedObject getInstance() { 
        return instance;
    }

    @Override 
    public String toString() {
        return "<UNDEFINED>";
    }
}
