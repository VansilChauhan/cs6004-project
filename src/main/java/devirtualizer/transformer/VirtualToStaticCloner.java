package devirtualizer.transformer;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import devirtualizer.OptimizationState;
import soot.Body;
import soot.Local;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.UnitBox;
import soot.Value;
import soot.ValueBox;
import soot.jimple.IdentityStmt;
import soot.jimple.Jimple;
import soot.jimple.ThisRef;

public class VirtualToStaticCloner extends SceneTransformer {
    private final OptimizationState state;

    public VirtualToStaticCloner(OptimizationState state) {
        this.state = state;
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        Map<SootMethod, SootMethod> originalToStaticMap = state.getOriginalToStaticMap();
        Set<SootMethod> methodsToTransform = Set.copyOf(state.getMonomorphicCallSites().values());

        for (SootMethod method : methodsToTransform) {
            if (originalToStaticMap.containsKey(method)) {
                continue;
            }

            SootMethod staticMethod = clone(method);
            if (staticMethod != null) {
                originalToStaticMap.put(method, staticMethod);
            }
        }
    }

    public static SootMethod clone(SootMethod vm) {
        if(!isValid(vm)) {
            System.err.println("Can't transform method " + vm + " to static method.");
            return null;
        }
        
        // 1. Create new static method 
        SootClass declaringClass = vm.getDeclaringClass();
        List<Type> newParameterTypes = new ArrayList<>(vm.getParameterTypes());
        newParameterTypes.add(declaringClass.getType());
        int newModifiers = Modifier.STATIC | vm.getModifiers();
        SootMethod newMethod = new SootMethod(vm.getName() + "_static", newParameterTypes, vm.getReturnType(), newModifiers);
        declaringClass.addMethod(newMethod);
        
        // 2. Clone the body
        Body body = vm.retrieveActiveBody();
        Body newBody = Jimple.v().newBody(newMethod);
        newMethod.setActiveBody(newBody);

        // 3. Clone locals
        Map<Local, Local> localMap = new HashMap<>();
        for(Local local : body.getLocals()) {
            Local newLocal = Jimple.v().newLocal(local.getName(), local.getType());
            localMap.put(local, newLocal);
            newBody.getLocals().add(newLocal);
        }

        // 4. Clone statements
        Map<Unit, Unit> unitMap = new HashMap<>();
        for(Unit u : body.getUnits()) {
            Unit newUnit;
            if(u instanceof IdentityStmt && ((IdentityStmt) u).getRightOp() instanceof ThisRef) {
                IdentityStmt id_stmt = (IdentityStmt) u;
                newUnit = Jimple.v().newIdentityStmt(
                    localMap.get(id_stmt.getLeftOp()), 
                    Jimple.v().newParameterRef(declaringClass.getType(), vm.getParameterCount())
                );
            } else {
                newUnit = (Unit) u.clone();
            }

            for(ValueBox vb : newUnit.getUseAndDefBoxes()) {
                Value v = vb.getValue();
                if(v instanceof Local && localMap.containsKey((Local) v)) {
                    vb.setValue(localMap.get((Local) v));
                }
            }

            newBody.getUnits().add(newUnit);
            unitMap.put(u, newUnit);
        }

        // 5. Fix branches 
        for(Unit u : newBody.getUnits()) {
            for(UnitBox ub : u.getUnitBoxes()) {
                if(unitMap.containsKey(ub.getUnit())) {
                    ub.setUnit(unitMap.get(ub.getUnit()));
                }
            }
        }
        
        return newMethod;
    }

    private static boolean isValid(SootMethod sm) {
        if(sm.isStatic() | sm.isAbstract() | sm.isNative() | sm.isSynchronized())
            return false;
        if(sm.retrieveActiveBody() == null) 
            return false;

        return true;
    }

}
