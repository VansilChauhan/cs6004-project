package devirtualizer;

import soot.*;

import devirtualizer.analyser.Analyser;
import devirtualizer.analyser.CallGraphBasedAnalysis;
import devirtualizer.analyser.InterPTABasedAnalyser;
import devirtualizer.StaticAnalyser;
import devirtualizer.transformer.CallSiteTransformer;
import devirtualizer.transformer.VirtualToStaticCloner;

public class Main {
    public static void main(String[] args) {
        ApplicationArguments appArgs = new ApplicationArguments(args);

        String[] sootArgs = {
            "-cp", appArgs.getClassPath(),
            "-pp",
            "-w",
            "-allow-phantom-refs",
            "-no-bodies-for-excluded", 
            "-keep-line-number",
            "-f", "c",
            "-main-class", appArgs.getMainClassName(), 
            "-process-dir", appArgs.getClassPath()
        };

        Analyser analyser = getAnalyser(appArgs.getLevel());
        OptimizationState state = new OptimizationState();

        Pack pack = PackManager.v().getPack("wjtp");
        pack.add(new Transform("wjtp.staticAnalyser", new StaticAnalyser(analyser, state)));
        pack.add(new Transform("wjtp.virtualToStaticCloner", new VirtualToStaticCloner(state)));
        pack.add(new Transform("wjtp.callSiteTransformer", new CallSiteTransformer(state)));

        soot.Main.main(sootArgs);

        int total, monomorphic;
        total = state.getTotalCallSites();
        monomorphic = state.getMonomorphicCallSites().size();
        System.out.println("Callsites transformed " + monomorphic + "/" + total + " (" + (monomorphic * 100.0 / total) + "%)");
        
    }

    private static Analyser getAnalyser(int level) {
        switch (level) {
            case 1:
                return new CallGraphBasedAnalysis();
            case 2:
                return new InterPTABasedAnalyser();
            default:
                throw new IllegalArgumentException("Invalid level: " + level);
        }
    }
}
