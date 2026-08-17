package devirtualizer.PTAToolkit;

import java.util.HashSet;
import java.util.Set;

import javax.management.openmbean.ArrayType;

import soot.RefType;
import soot.SootClass;
import soot.SootField;
import soot.jimple.NewExpr;

public class RealObject extends HeapObject implements Comparable {
    private final int line_no;
    private final NewExpr expr;

    public RealObject(int line_no, NewExpr expr) {
        this.line_no = line_no;
        this.expr = expr;
    }

    public SootClass getSootClass() {
        return expr.getBaseType().getSootClass();
    }

    // public Set<ObjectField> getFields() {
    //     Set<ObjectField> fields = new HashSet<>();
    //     SootClass sc
    //     return new HashSet<>();
    // }

    @Override
    public int compareTo(Object obj) {
        if(!(obj instanceof RealObject))    return 0;
        
        RealObject other = (RealObject) obj;
        if(other.line_no == line_no)    return 0;
        return (line_no > other.line_no) ? 1 : -1;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)     return false;
        if(!(obj instanceof RealObject))    return false;
        if(obj == this)     return true;

        RealObject real_obj = (RealObject) obj;
        return expr.equals(real_obj.expr) && (line_no == real_obj.line_no);
    }

    @Override
    public int hashCode() {
        return line_no * 31 + expr.hashCode();
    }

    @Override
    public String toString() {
        return "O" + line_no;
    }
}
