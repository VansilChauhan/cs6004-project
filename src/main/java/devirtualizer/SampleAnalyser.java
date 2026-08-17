package devirtualizer;

import java.util.Map;

import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;

public class SampleAnalyser extends SceneTransformer {
    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        for(SootClass sc : Scene.v().getApplicationClasses()) {
            System.out.println("Class " + sc + ": ");
            for(SootField sf : sc.getFields()) {
                System.out.println("\tField: " + sf + " ");
            }
            for(SootMethod sm : sc.getMethods()) {
                if(sm.isJavaLibraryMethod()) continue;
                System.out.println("\tMethod: " + sm + " ");
            }
            System.out.println();
        }
    }
}
