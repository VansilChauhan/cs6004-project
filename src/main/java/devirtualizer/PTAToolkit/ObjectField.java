package devirtualizer.PTAToolkit;

import soot.SootField;

public class ObjectField {
    private final String declaringClassName, fieldName;

    public ObjectField(String declaringClassName, String fieldName) {
        this.declaringClassName = declaringClassName;
        this.fieldName = fieldName;
    }

    public ObjectField(SootField sf) {
        declaringClassName = sf.getDeclaringClass().getName();
        fieldName = sf.getName();
    }
    

    @Override
    public boolean equals(Object obj) {
        if(obj == null)     return false;
        if(!(obj instanceof ObjectField))    return false;
        if(obj == this)     return true;

        ObjectField obj_field = (ObjectField) obj;
        return obj_field.fieldName.equals(fieldName) && obj_field.declaringClassName.equals(declaringClassName);
    }

    @Override
    public int hashCode() {
        return declaringClassName.hashCode() * 31 + fieldName.hashCode();
    }

    @Override
    public String toString() {
        return declaringClassName + "." + fieldName;
    }
}
