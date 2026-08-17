package devirtualizer.PTAToolkit;

import soot.RefType;
import soot.SootClass;
import soot.jimple.ParameterRef;

public class ParameterObject extends DummyObject {
    private final ParameterRef ref;

    public ParameterObject(ParameterRef thisReference) {
        ref = thisReference;
    }

    @Override
    public SootClass getSootClass() {
        if(ref.getType() instanceof RefType)
            return ((RefType) ref.getType()).getSootClass();
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)     return false;
        if(!(obj instanceof ParameterObject))    return false;
        if(obj == this)     return true;

        ParameterObject ParameterObject = (ParameterObject) obj;
        return ParameterObject.ref.equals(ref);
    }

    @Override
    public int hashCode() {
        return ref.hashCode();
    }

    @Override
    public String toString() {
        return "param" + ref.getIndex();
    }
}
