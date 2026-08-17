import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import soot.G;
import soot.PackManager;
import soot.Printer;
import soot.Scene;
import soot.SootClass;
import soot.options.Options;

public class TranslateToJimple {
    public static void main(String[] args) throws Exception {
        String inputDir = parseInputDir(args);
        if (inputDir == null) {
            printUsage();
            System.exit(1);
        }

        Path inputPath = Paths.get(inputDir).toAbsolutePath().normalize();
        if (!Files.isDirectory(inputPath)) {
            System.err.println("Input directory not found: " + inputPath);
            System.exit(1);
        }

        translate(inputPath);
    }

    private static void translate(Path inputPath) throws IOException {
        G.reset();

        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_process_dir(Collections.singletonList(inputPath.toString()));
        Options.v().set_whole_program(false);

        Scene.v().loadNecessaryClasses();
        PackManager.v().runPacks();

        String dirName = inputPath.getFileName() == null ? "program" : inputPath.getFileName().toString();
        Path parentDir = inputPath.getParent() == null ? Paths.get(".").toAbsolutePath().normalize() : inputPath.getParent();
        Files.createDirectories(parentDir);
        Path outputPath = parentDir.resolve(dirName + ".jimple").normalize();

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8))) {
            for (SootClass sootClass : getApplicationClasses()) {
                Printer.v().printTo(sootClass, writer);
                writer.println();
                writer.println();
            }
        }

        System.out.println("Jimple written to " + outputPath);
    }

    private static List<SootClass> getApplicationClasses() {
        return Scene.v().getApplicationClasses()
            .stream()
            .sorted(Comparator.comparing(SootClass::getName))
            .collect(Collectors.toList());
    }

    private static String parseInputDir(String[] args) {
        if (args.length != 1) {
            return null;
        }
        return args[0];
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("java -cp <class-path> TranslateToJimple <input-class-dir>");
        System.out.println("Output is written next to the input directory as <input-dir-name>.jimple");
    }
}
