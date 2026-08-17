package devirtualizer.analyser;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import devirtualizer.PTAToolkit.HeapObject;
import devirtualizer.PTAToolkit.PointsToAnalyser;
import devirtualizer.PTAToolkit.PointsToSet;
import devirtualizer.PTAToolkit.PointsToState;
import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;

public class InterPTABasedAnalyser extends Analyser {
    CallGraph cg;
    @Override
    public void performAnalysis() {
        cg = Scene.v().getCallGraph();
        // 1. Get the main method of program under analysis
        SootMethod mainMethod = Scene.v().getMainMethod();
        if(mainMethod == null) {
            System.err.println("Error: Failed to locate main entry point.");
            System.exit(1);
        }

        // 2. Perform an Inter-procedural points-to analysis over it
        PointsToAnalyser inter_pta = new PointsToAnalyser(mainMethod.retrieveActiveBody());
        inter_pta.print();

        // 3. Collect target objects for each callsite
        Map<Unit, Set<SootMethod>> callSites = new HashMap<>();
        getCallSiteToTargetMapping(mainMethod, inter_pta, callSites);

        // 4. Identify monomorphic callsites and record them
        for(Unit u : callSites.keySet()) {
            Set<SootMethod> targetSet = callSites.get(u);
            if(targetSet.size() == 1) {
                monomorphicCallSites.put(u, targetSet.iterator().next());
            }
        }

    }

    private void getCallSiteToTargetMapping(SootMethod method, PointsToAnalyser pta, Map<Unit, Set<SootMethod>> targetMap) {
        Body body = method.retrieveActiveBody();
        if(body == null)    return;

        for(Unit u : body.getUnits()) {
            Stmt stmt = (Stmt) u;
            if(!stmt.containsInvokeExpr())  continue;

            InvokeExpr expr = (InvokeExpr) stmt.getInvokeExpr();
            if(!(expr instanceof InstanceInvokeExpr))     
                continue;
            if(expr instanceof SpecialInvokeExpr)   continue;
            if(expr.getMethod().isJavaLibraryMethod())  continue;
            
            // 1. Update targetMap for this callsite
            Map<SootMethod, PointsToAnalyser> targets = pta.getTargets(stmt);
            if(targets == null) {
                System.err.println("Warning: target set returned null for " + stmt);
                continue;
            }
            if(targetMap.containsKey(stmt) == false) {
                targetMap.put(stmt, new HashSet<>());
            }
            targetMap.get(stmt).addAll(targets.keySet());

            // 2. call recursively for the targets
            for(SootMethod target : targets.keySet()) {
                getCallSiteToTargetMapping(target, pta.getPTAFor(stmt, target), targetMap);
            }
            
        }
    }

}
