package devirtualizer.PTAToolkit;

import soot.RefType;
import soot.SootClass;
import soot.jimple.ThisRef;

public class ThisObject extends DummyObject {
    private final ThisRef ref;

    public ThisObject(ThisRef thisReference) {
        ref = thisReference;
    }

    @Override
    public SootClass getSootClass() {
        return ((RefType) ref.getType()).getSootClass();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)     return false;
        if(!(obj instanceof ThisObject))    return false;
        if(obj == this)     return true;

        ThisObject thisObject = (ThisObject) obj;
        return thisObject.ref.equals(ref);
    }

    @Override
    public int hashCode() {
        return ref.hashCode();
    }

    @Override
    public String toString() {
        return "this";
    }
}
