package devirtualizer.PTAToolkit;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import polyglot.ast.Assign;
import polyglot.ast.New;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.NewExpr;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.ThisRef;

public class Util {
    public static Set<ObjectField> getFields(SootClass sc) {
        if(sc == null) {    // Array Type
            return new HashSet<>();
        }
        Set<ObjectField> fields = new HashSet<>();
        while(sc.isApplicationClass()) {
            fields.addAll(sc.getFields().stream().map(obj -> new ObjectField(obj)).collect(Collectors.toSet()));
            sc = sc.getSuperclass();
        }
        return fields;
    }

    public static void printPTA(SootMethod sm, PrintStream out_stream) {
        Body body = sm.retrieveActiveBody();
        if(body == null)    {
            // System.out.println("---- Method body not available ----");
            out_stream.println("---- Method body not available ----");
            out_stream.close();
            return;
        }
        
        String className, methodName;
        className = sm.getDeclaringClass().getName();
        methodName = sm.getName();
        out_stream.println("------- " + className + "." + methodName + " -------");

        PointsToAnalyser pta = new PointsToAnalyser(body);
        

        for(Unit u : body.getUnits()) {
            PointsToState in, out;
            in = pta.getFlowBefore(u);
            out = pta.getFlowAfter(u);

            out_stream.println(u.getJavaSourceStartLineNumber() + ") stmt: " + u);
            out_stream.println("IN");
            out_stream.println(in);
            out_stream.println("OUT");
            out_stream.println(out.equals(in) ? "--same as in ---" : out);
            out_stream.println("\n");
        }

        out_stream.println("------------------------------------");
    }

    public static Set<HeapObject> getAllHeapObjects(Body body) {
        Set<HeapObject> result = new HashSet<>();
        result.addAll(getAllDummyObjects(body));
        result.addAll(getAllAllocatedObjects(body));
        return result;
    }

    public static Set<RealObject> getAllAllocatedObjects(Body body) {
        Set<RealObject> result = new HashSet<>();
        for(Unit u : body.getUnits()) {
            if(!(u instanceof AssignStmt)) continue;
            AssignStmt assign_stmt = (AssignStmt) u;
            if(assign_stmt.getRightOp() instanceof NewExpr) {
                int line_no = u.getJavaSourceStartLineNumber();
                result.add(new RealObject(line_no, (NewExpr) assign_stmt.getRightOp()));
            }
        }
        return result;
    }

    public static Set<DummyObject> getAllDummyObjects(Body body) {
        Set<DummyObject> result = new HashSet<>();
        for(Unit u : body.getUnits()) {
            if(!(u instanceof IdentityStmt)) continue;

            IdentityStmt stmt = (IdentityStmt) u;
            if(stmt.getRightOp() instanceof ThisRef) {
                result.add(new ThisObject((ThisRef) stmt.getRightOp()));
                continue;
            }
            if(stmt.getRightOp() instanceof ParameterRef) {
                result.add(new ParameterObject((ParameterRef) stmt.getRightOp()));
                continue;
            }
        }
        return result;
    }

    public static Set<HeapObject> getReachableObjects(PointsToSet rootSet, PointsToState in) {
        
        Queue<HeapObject> que = new ArrayDeque<>();
        Set<HeapObject> visited = new HashSet<>();

        for(HeapObject obj : rootSet.getHeapObjects()) {
            que.add(obj);
            visited.add(obj);
        }

        Set<HeapObject> result = new HashSet<>();
        while(!que.isEmpty()) {
            HeapObject obj = que.poll();
            result.add(obj);

            for(HeapObject next : in.getPointeeSet(obj).getHeapObjects()) {
                if(visited.contains(next))  continue;

                que.add(obj);
                visited.add(obj);
            }
        }
        return result;
    }


    public static ParameterRef getParameterRef(int index, Body body) {
        for(Unit u : body.getUnits()) {
            if(!(u instanceof IdentityStmt))    continue;
            IdentityStmt identity_stmt = (IdentityStmt) u;
            if(!(identity_stmt.getRightOp() instanceof ParameterRef))     continue;
            ParameterRef param = (ParameterRef) identity_stmt.getRightOp();
            if(param.getIndex() == index)   return param;
        }

        return null;
    }

    public static ThisRef getThisRef(Body body) {
        for(Unit u : body.getUnits()) {
            if(!(u instanceof IdentityStmt))    continue;
            IdentityStmt identity_stmt = (IdentityStmt) u;
            if(identity_stmt.getRightOp() instanceof ThisRef)
                return (ThisRef) identity_stmt.getRightOp();
        }
        return null;
    }

    public static NewExpr getNewExpr(Unit u) {
        if(u instanceof AssignStmt) {
            AssignStmt assign_stmt = (AssignStmt) u;
            if(assign_stmt.getRightOp() instanceof NewExpr) {
                return (NewExpr) assign_stmt.getRightOp();
            }
        }
        return null; 
    }

    public static Set<RealObject> getAllocatedObjects(Body body) {
        if(body == null)    return new HashSet<>();
        
        Set<RealObject> result = new HashSet<>();
        for(Unit u : body.getUnits()) {
            if(u instanceof AssignStmt && ((AssignStmt) u).getRightOp() instanceof NewExpr) {
                AssignStmt assign_stmt = (AssignStmt) u;
                NewExpr expr = (NewExpr) assign_stmt.getRightOp();
                result.add(new RealObject(u.getJavaSourceStartLineNumber(), expr));
            }
        }
        return result;
    }

    public static String getFQN(SootMethod sm) {
        return sm.getDeclaringClass().getName() + "." + sm.getName();
    }

    public static void deleteDirectory(String file_name) throws IOException {
        Path path = Path.of(file_name);
        if(!Files.exists(path))     return;

        Files.walk(path)
        .sorted(Comparator.reverseOrder()) // deleting children first
        .forEach(p -> {
            try {
                Files.delete(p);
            } catch(IOException e) {

            }
        });
    }

}
