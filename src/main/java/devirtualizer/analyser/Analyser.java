package devirtualizer.analyser;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import soot.SootMethod;
import soot.Unit;

public abstract class Analyser {
    Map<Unit, SootMethod> monomorphicCallSites = new HashMap<>();

    
    public abstract void performAnalysis();

    public Map<Unit, SootMethod> getMonomorphicCallSites() {
        return new HashMap<>(monomorphicCallSites);
    }
}
