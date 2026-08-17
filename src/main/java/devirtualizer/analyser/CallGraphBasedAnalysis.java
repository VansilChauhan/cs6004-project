package devirtualizer.analyser;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class CallGraphBasedAnalysis extends Analyser {
    CallGraph callGraph;
    @Override
    public void performAnalysis() {
        callGraph = Scene.v().getCallGraph();

        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (!sootMethod.isConcrete()) {
                    continue;
                }

                Body body = sootMethod.retrieveActiveBody();
                if (body == null) {
                    continue;
                }

                for (Unit unit : body.getUnits()) {
                    Stmt stmt = (Stmt) unit;
                    if (!stmt.containsInvokeExpr()) {
                        continue;
                    }

                    InvokeExpr invokeExpr = stmt.getInvokeExpr();
                    if (!(invokeExpr instanceof VirtualInvokeExpr)) {
                        continue;
                    }

                    Set<SootMethod> targets = new HashSet<>();
                    Iterator<Edge> edges = callGraph.edgesOutOf(unit);
                    while (edges.hasNext()) {
                        SootMethod target = edges.next().tgt();
                        if (target != null && target.isConcrete()) {
                            targets.add(target);
                            if (targets.size() > 1) {
                                break;
                            }
                        }
                    }

                    if (targets.size() == 1) {
                        monomorphicCallSites.put(unit, targets.iterator().next());
                    }
                }
            }
        }
    }
}
