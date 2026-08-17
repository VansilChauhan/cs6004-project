package devirtualizer.PTAToolkit;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import polyglot.ast.Return;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import soot.toolkits.scalar.Pair;

public class PointsToAnalyser extends ForwardFlowAnalysis<Unit, PointsToState> {
    private final UnitGraph graph;
    private final SootMethod method;
    private final Set<HeapObject> objects;
    private final Set<Local> locals;
    private final PointsToSet bottomSet;
    private final PointsToState entryState;
    private final String className, methodName;
    private final PointsToSet thisPointsToSet;
    private final List<PointsToSet> paramPointsToSet;
    private final Map<Unit, Map<SootMethod, PointsToAnalyser>> callSitePTA = new HashMap<>();
    private final List<Pair<Integer, SootMethod>> callStack;

    public PointsToAnalyser(Body body) {
        super(new BriefUnitGraph(body));

        method = body.getMethod();
        graph = new BriefUnitGraph(body);
        className = body.getMethod().getDeclaringClass().getName();
        methodName = body.getMethod().getName();

        objects = Util.getAllHeapObjects(body);
        locals = new HashSet<>(body.getLocals());

        bottomSet = new PointsToSet(objects);
        bottomSet.add(NullObject.getInstance());
        PointsToSet.maxSize = bottomSet.size();

        entryState = new PointsToState(locals, objects);

        thisPointsToSet = method.isStatic() ? null : new PointsToSet(new ThisObject(Util.getThisRef(body)));
        paramPointsToSet = new ArrayList<>();
        for(int i=0; i<body.getMethod().getParameterCount(); i++) {
            paramPointsToSet.add(new PointsToSet(new ParameterObject(Util.getParameterRef(i, body))));
        }

        callStack = new ArrayList<>();
        callStack.add(new Pair<Integer,SootMethod>(0, method));
        
        doAnalysis();
    }

    public PointsToAnalyser(Body body, PointsToSet thisPTS, List<PointsToSet> paramPTS, PointsToState entry, List<Pair<Integer,SootMethod>> callStack) {
        super(new BriefUnitGraph(body));

        method = body.getMethod();
        graph = new BriefUnitGraph(body);
        className = body.getMethod().getDeclaringClass().getName();
        methodName = body.getMethod().getName();

        locals = new HashSet<>(body.getLocals());
        objects = Util.getAllHeapObjects(body);
        
        bottomSet = new PointsToSet(objects);
        bottomSet.add(NullObject.getInstance());
        
        // todo: add entry objects tree also in the bottom set
        PointsToSet.maxSize = bottomSet.size();

        entryState = new PointsToState(locals, objects);
        entryState.copyHeap(entry);
        entryState.copyEscapeSet(entry);

        thisPointsToSet = thisPTS;
        paramPointsToSet = paramPTS;

        this.callStack = new ArrayList<>(callStack);

        doAnalysis();
    }

    public PointsToState getMergedOutState() {
        Body body = method.retrieveActiveBody();
        if(body == null)    return new PointsToState();
        
        
        PointsToState result = new PointsToState(locals, objects);
        for(Unit u : body.getUnits()) {
            if(u instanceof ReturnStmt) {
                // result.merge(result, getFlowAfter(u));
                result.mergeHeap(getFlowAfter(u));
                result.mergeEscapeSet(getFlowAfter(u));
            }
        }
        return result;
    }

    public PointsToSet getReturnSet() {
        if(!(method.getReturnType() instanceof RefType))    return new PointsToSet();

        Body body = method.retrieveActiveBody();
        if(body == null)    return new PointsToSet();
        

        PointsToSet result = new PointsToSet();
        for(Unit u : body.getUnits()) {
            if(!(u instanceof ReturnStmt))  continue;

            ReturnStmt ret_stmt = (ReturnStmt) u;
            if(ret_stmt.getOp() instanceof NullConstant) {
                result.add(NullObject.getInstance());
            } else if(ret_stmt.getOp() instanceof Local) {
                Local local = (Local) ret_stmt.getOp();
                PointsToState in = getFlowBefore(ret_stmt);
                result.addAll(in.getPointeeSet(local));
            } else {
                
            }
        }
        return result;
    }

    public PointsToAnalyser getPTAFor(Unit u, SootMethod target) {
        if(!callSitePTA.containsKey(u))     return null;
        if(!callSitePTA.get(u).containsKey(target))     return null;
        return callSitePTA.get(u).get(target);
    }
    
    public Map<SootMethod, PointsToAnalyser> getTargets(Unit u) {
        return callSitePTA.get(u);
    }
    @Override
    protected PointsToState newInitialFlow() {
        return new PointsToState(locals, objects);
    }

    @Override
    protected PointsToState entryInitialFlow() {
        return new PointsToState(entryState);
    }

    protected void copy(PointsToState src, PointsToState dst) {
        dst.copy(src);
    }

    protected void merge(PointsToState in1, PointsToState in2, PointsToState out) {
        out.merge(in1, in2);
    }

    protected void flowThrough(PointsToState in, Unit u, PointsToState out) {
        // if(graph.getHeads().contains(u) || graph.getPredsOf(u).isEmpty()) {
        //     System.out.println("In " + className + "." + methodName + " head " + u + "equals: ");
        //     System.out.println(in);
        // }

        out.copy(in);
        Stmt stmt = (Stmt) u;

        if(stmt instanceof IdentityStmt) {
            handleIdentityStmt((IdentityStmt) stmt, in, out);
            return;
        }
        if(stmt instanceof AssignStmt) {
            handleAssignStmt((AssignStmt) stmt, in, out);
            return;
        }
        if(stmt instanceof InvokeStmt) {
            handleInvokeExpr(stmt, ((InvokeStmt) stmt).getInvokeExpr(), in, out);
            return;
        }
        if(stmt instanceof ReturnStmt) {
            handleReturnStmt((ReturnStmt) stmt, in, out);
            return;
        }
    }

    public PointsToSet getBottomSet() {
        return bottomSet.copy();
    }

    /* Private methods to handle individual statements */
    private void handleIdentityStmt(IdentityStmt stmt, PointsToState in, PointsToState out) {
        out.copy(in);

        if(!(stmt.getLeftOp() instanceof Local))    return;
        Local lhs = (Local) stmt.getLeftOp();
        Value rhs = stmt.getRightOp();

        if(!(lhs.getType() instanceof RefType))     return;

        if(rhs instanceof ThisRef) {
            out.update(lhs, thisPointsToSet);
            return;
        }
        if(rhs instanceof ParameterRef) {
            int index = ((ParameterRef) rhs).getIndex();
            out.update(lhs, paramPointsToSet.get(index));
            return;
        }
    }

    private void handleAssignStmt(AssignStmt stmt, PointsToState in, PointsToState out) {
        if(stmt.getRightOp() instanceof NewExpr) {
            Local lhs = (Local) stmt.getLeftOp();
            NewExpr rhs = (NewExpr) stmt.getRightOp();
            out.update(lhs, new PointsToSet(new RealObject(stmt.getJavaSourceStartLineNumber(), rhs)));
            return;
        }
        if(stmt.getLeftOp() instanceof Local && stmt.getRightOp() instanceof Local) {
            Local lhs, rhs;
            lhs = (Local) stmt.getLeftOp();
            rhs = (Local) stmt.getRightOp();
            out.update(lhs, rhs);
            return;
        }
        if(stmt.getLeftOp() instanceof FieldRef) {
            handleFieldStore(stmt, in, out);
            return;
        }
        if(stmt.getRightOp() instanceof FieldRef) {
            handleFieldLoad(stmt, in, out);
            return;
        }
        if(stmt.getRightOp() instanceof InvokeExpr) {
            Local lhs = (Local) stmt.getLeftOp();
            handleInvokeExpr(stmt, (InvokeExpr) stmt.getRightOp(), in, out);

            PointsToSet returnSet = new PointsToSet();
            for(PointsToAnalyser pta : callSitePTA.get(stmt).values()) {
                returnSet.addAll(pta.getReturnSet());
            }
            out.update(lhs, returnSet);
            return;
        }
    }

    private void handleInvokeExpr(Unit u, InvokeExpr expr, final PointsToState in, PointsToState out) {
        if(expr instanceof SpecialInvokeExpr && expr.getArgCount() == 0)   return;      // skip default constructors
        if(expr.getMethod().isJavaLibraryMethod())  return;     // skip library methods
        
        
        // System.out.println("handleInvokeExpr: " + u);
        // System.out.println("call string: " + getCallString());
        // 1. Get base context
        PointsToSet thisPTS;
        if(expr instanceof InstanceInvokeExpr) {
            InstanceInvokeExpr iie = (InstanceInvokeExpr) expr;
            if(!(iie.getBase() instanceof Local))   return;
            Local base = (Local) iie.getBase();
            thisPTS = in.getPointeeSet(base);
        } else {
            thisPTS = null;
        } 

        // 2. Get Arguments' context
        List<PointsToSet> paramPTS = new ArrayList<>();
        for(int i=0; i<expr.getArgCount(); i++) {
            Value arg = expr.getArg(i);
            Type argType = arg.getType();
            
            if(!(argType instanceof RefType)) { // ignore non-reftype arguments
                paramPTS.add(null);
                continue;
            }
            if(arg instanceof Local) {
                paramPTS.add(in.getPointeeSet((Local) arg));
            } else if(arg instanceof NullConstant) {
                paramPTS.add(new PointsToSet(NullObject.getInstance()));
            } else {
                // do not process other type of arguments
                paramPTS.add(new PointsToSet());
            }
        }


        // 3. Collect effects of all callee candidates using only the current PTA state.
        callSitePTA.put(u, new HashMap<>());
        Set<SootMethod> feasibleTargets = getFeasibleTargets(expr, in);
        for(SootMethod target : feasibleTargets) {
            if(target.isJavaLibraryMethod())    continue;
            Body body = target.retrieveActiveBody();
            if(body == null)    continue;
            
            List<Pair<Integer, SootMethod>> newCallStack = new ArrayList<Pair<Integer, SootMethod>>(callStack);
            newCallStack.add(new Pair<Integer,SootMethod>(u.getJavaSourceStartLineNumber(), target));
            PointsToAnalyser pta = new PointsToAnalyser(body, thisPTS, paramPTS, in, newCallStack);
            callSitePTA.get(u).put(target, pta);
        }


        // 4. Merge all effects to generate out 
        for(PointsToAnalyser pta : callSitePTA.get(u).values()) {
            out.merge(out, pta.getMergedOutState());
        }
    }

    private Set<SootMethod> getFeasibleTargets(InvokeExpr expr, PointsToState in) {
        Set<SootMethod> targets = new HashSet<>();

        if(expr instanceof StaticInvokeExpr || expr instanceof SpecialInvokeExpr) {
            targets.add(expr.getMethod());
            return targets;
        }

        if(!(expr instanceof InstanceInvokeExpr)) {
            targets.add(expr.getMethod());
            return targets;
        }

        Value baseValue = ((InstanceInvokeExpr) expr).getBase();
        if(!(baseValue instanceof Local)) {
            return targets;
        }

        Local base = (Local) baseValue;
        PointsToSet basePointsTo = in.getPointeeSet(base);
        Hierarchy hierarchy = Scene.v().getActiveHierarchy();

        for(HeapObject obj : basePointsTo.getHeapObjects()) {
            SootClass receiverClass = obj.getSootClass();
            if(receiverClass == null)    continue;

            try {
                if(expr instanceof InterfaceInvokeExpr && receiverClass.isInterface()) {
                    continue;
                }

                SootMethod target = hierarchy.resolveConcreteDispatch(receiverClass, expr.getMethod());
                if(target != null && target.isConcrete()) {
                    targets.add(target);
                }
            } catch(RuntimeException e) {
                // Ignore unresolved dispatches for this receiver object.
            }
        }

        return targets;
    }

    private void handleFieldLoad(AssignStmt stmt, PointsToState in, PointsToState out) {
        if(!(((FieldRef) stmt.getRightOp()).getType() instanceof RefType))
            return; // skip non-RefType 

        if(!(stmt.getLeftOp() instanceof Local))    return;
        Local lhs = (Local) stmt.getLeftOp();

        if(stmt.getRightOp() instanceof StaticFieldRef) {
            if(!(stmt.getLeftOp() instanceof Local))
                return;

            out.update(lhs, new PointsToSet(in.getEscapeSet()));
            return;
        }

        if(stmt.getRightOp() instanceof InstanceFieldRef) {
            InstanceFieldRef rhs = (InstanceFieldRef) stmt.getRightOp();
            
            if(!(rhs.getBase() instanceof Local))   return;
            Local base = (Local) rhs.getBase();
            ObjectField field = new ObjectField(rhs.getField());

            PointsToSet pts_base = in.getPointeeSet(base);
            PointsToSet pts_target = new PointsToSet();
            for(HeapObject obj : pts_base.getHeapObjects()) {
                SootClass sc = obj.getSootClass();
                pts_target.addAll(in.getPointeeSet(obj, field));
            }

            out.update(lhs, pts_target);
            return;
        }
    }

    private void handleFieldStore(AssignStmt stmt, PointsToState in, PointsToState out) {
        if(!(((FieldRef) stmt.getLeftOp()).getType() instanceof RefType))
            return; // skip primitive fields

        if(stmt.getLeftOp() instanceof StaticFieldRef) {
            if(stmt.getRightOp() instanceof Local) {
                Local rhs = (Local) stmt.getRightOp();
                // PointsToSet pointeeSet = in.getPointeeSet((Local) stmt.getLeftOp());
                out.escape(in.getPointeeSet(rhs), in);
                return;
            }
        }

        if(stmt.getLeftOp() instanceof InstanceFieldRef) {
            // 1. Collect points-to set of rhs
            InstanceFieldRef lhs = (InstanceFieldRef) stmt.getLeftOp();
            PointsToSet resultSet = new PointsToSet();
            if(stmt.getRightOp() instanceof Local) {
                resultSet.addAll(in.getPointeeSet((Local) stmt.getRightOp()));
            } else {
                resultSet.add(NullObject.getInstance());
            }

            // 2. Perform strong / weak update on destination
            ObjectField field = new ObjectField(lhs.getField());
            PointsToSet pts_base = in.getPointeeSet((Local) lhs.getBase());
            Set<HeapObject> pts_base_set = pts_base.getHeapObjects();
            if(pts_base_set.size() == 1) {    // strong update
                HeapObject target = pts_base_set.iterator().next();
                out.strongUpdate(target, field, resultSet);
            } else { // weak update 
                for(HeapObject obj : pts_base_set) {
                    out.weakUpdate(obj, field, resultSet);
                }
            }

            
            // 3. If the base object escapes, then also the objects reachable from 'rhs' escape
            boolean base_escapes = false;
            for(HeapObject obj : pts_base_set) {
                if(in.doesEscape(obj)) {
                    base_escapes = true;
                    break;
                }
            }
            if(base_escapes) {
                out.escape(resultSet, in);
            }
        }
    }

    private void handleReturnStmt(ReturnStmt stmt, PointsToState in, PointsToState out) {
        
    }

    private String getCallString() {
        StringBuilder sb = new StringBuilder();
        for(Pair<Integer, SootMethod> p : callStack) {
            SootMethod sm = p.getO2();
            sb.append(p.getO1() + "." + Util.getFQN(sm) + "-");
        }
        return sb.toString();
    }
    

    public void print() {
        PrintStream out_stream = null;

        try {
            // 1. Open a file to print
            final String file_name = getCallString();
            File file = new File("pta-log/" + file_name + ".pta");
            out_stream = new PrintStream(file);

            // 2. Print entry conditions first
            out_stream.println("entry initial flow: \n" + entryState);
            out_stream.println("@this: " + thisPointsToSet);
            for(int i=0; i<paramPointsToSet.size(); i++) {
                out_stream.println("@param-" + i + ": " + paramPointsToSet.get(i));
            }
            out_stream.println("returnSet: " + getReturnSet());
            out_stream.println("\n");

            // 3. Print the points-to analysis now
            out_stream.println("=================== Points To Analysis ===================== ");
            for(Unit u : method.retrieveActiveBody().getUnits()) {
                out_stream.println(u.getJavaSourceStartLineNumber() + ") stmt: " + u);
                PointsToState in, out;
                in = getFlowBefore(u);
                out = getFlowAfter(u);
                out_stream.println("In: \n" + getFlowBefore(u));
                if(out.equals(in)) {
                    out_stream.println("Out: ----- same as in ----- " );
                } else {
                    out_stream.println("Out: \n" + getFlowAfter(u));
                }
                out_stream.println("\n");
            }

            // 4. Print pta for all callees through current method
            for(Unit u : callSitePTA.keySet()) {
                for(PointsToAnalyser pta : callSitePTA.get(u).values()) {
                    pta.print();
                }
            }

            out_stream.close();
        } catch(IOException e) {
            // do nothing for silent failure
        }
    }
}
