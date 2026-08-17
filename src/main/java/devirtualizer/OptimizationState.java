package devirtualizer;

import java.util.HashMap;
import java.util.Map;

import soot.SootMethod;
import soot.Unit;

public class OptimizationState {
    private final Map<Unit, SootMethod> monomorphicCallSites = new HashMap<>();
    private final Map<SootMethod, SootMethod> originalToStaticMap = new HashMap<>();
    int totalCallSites = 0;

    public Map<Unit, SootMethod> getMonomorphicCallSites() {
        return monomorphicCallSites;
    }

    public Map<SootMethod, SootMethod> getOriginalToStaticMap() {
        return originalToStaticMap;
    }
    
    public int getTotalCallSites() {
        return totalCallSites;
    }

    public void incrementTotalCallSites() {
        totalCallSites++;
    }
}
