package devirtualizer.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import devirtualizer.OptimizationState;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.Stmt;

public class CallSiteTransformer extends SceneTransformer {
    private final OptimizationState state;

    public CallSiteTransformer(OptimizationState state) {
        this.state = state;
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        for (Map.Entry<soot.Unit, SootMethod> entry : state.getMonomorphicCallSites().entrySet()) {
            SootMethod staticMethod = state.getOriginalToStaticMap().get(entry.getValue());
            if (staticMethod == null) {
                continue;
            }

            transformCallSite((Stmt) entry.getKey(), staticMethod);
        }
    }

    public static void transformCallSite(Stmt s, SootMethod staticMethod) {
        InvokeExpr oldExpr = null;

        if (s instanceof InvokeStmt) {
            oldExpr = ((InvokeStmt) s).getInvokeExpr();
        } else if (s instanceof AssignStmt) {
            Value rhs = ((AssignStmt) s).getRightOp();
            if (rhs instanceof InvokeExpr) {
                oldExpr = (InvokeExpr) rhs;
            }
        }

        if (oldExpr == null || oldExpr.getMethod().isStatic()) {
            return;
        }

        List<Value> newArgs = new ArrayList<>();
        InstanceInvokeExpr iie = (InstanceInvokeExpr) oldExpr;
        newArgs.addAll(iie.getArgs());
        newArgs.add(iie.getBase());

        InvokeExpr newExpr = Jimple.v().newStaticInvokeExpr(
                staticMethod.makeRef(),
                newArgs
        );

        if (s instanceof InvokeStmt) {
            ((InvokeStmt) s).setInvokeExpr(newExpr);

        } else if (s instanceof AssignStmt) {
            ((AssignStmt) s).setRightOp(newExpr);
        }
    }

}
