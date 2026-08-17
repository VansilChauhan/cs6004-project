package devirtualizer;

import java.util.Map;

import devirtualizer.analyser.Analyser;
import soot.Body;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;

public class StaticAnalyser extends SceneTransformer {
    private final Analyser analyser;
    private final OptimizationState state;

    public StaticAnalyser(Analyser analyser, OptimizationState state) {
        this.analyser = analyser;
        this.state = state;
    }
    
    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        analyser.performAnalysis();
        state.getMonomorphicCallSites().clear();
        state.getMonomorphicCallSites().putAll(analyser.getMonomorphicCallSites());

        // count total callsites for reporting
        for(SootClass sc : Scene.v().getApplicationClasses()) {
            if(sc.isAbstract() || sc.isInterface())   continue;
            for(SootMethod sm : sc.getMethods()) {
                if(sm.isJavaLibraryMethod())    continue;
            

                // System.out.println("Analyzing method: " + sm.getSignature());
                Body body = sm.retrieveActiveBody();
                if(body == null)   continue;
                
                for(Unit u : body.getUnits()) {
                    Stmt stmt = (Stmt) u;                   

                    if(stmt.containsInvokeExpr()) {
                        InvokeExpr ie = stmt.getInvokeExpr();
                        if(ie instanceof SpecialInvokeExpr)   continue; // skip constructors

                        // System.out.println("Found callsite: " + ie.getMethod().getSignature());
                        state.incrementTotalCallSites(); 
                    }
                }
            }
        }
    }
}
