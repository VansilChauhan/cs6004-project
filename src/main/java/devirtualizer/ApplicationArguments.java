package devirtualizer;

import java.util.Iterator;




public class ApplicationArguments {
    private String classPath, mainClassName;
    private Integer level;   // Default level of analysis

    public ApplicationArguments(String[] args) {
        // parse the arguments here
        int idx = 0;
        if(args.length == 0) {
            System.out.println("usage: devirtualizer -c <arg> -d <arg> [-l <arg>]");
            System.out.println("\tdevirtualizer - Java monomorphic call-site analysis and optimization tool via code transformation");

            System.out.println("\t-d,--directory <arg>\tAbsolute path to application directory");
            System.out.println("\t-c,--main-fqnc <arg>\tApplication entry point main class fully qualified name (e.g. com.example.Main)");
            System.out.println("\t-l,--level <arg>\tLevel of optimization to perform");
            System.out.println("\t\t\t\t1: Call graph-based analysis");
            System.out.println("\t\t\t\t2: Inter-procedural PTA-based analysis");

            System.exit(-1);
        }
        
        while(idx < args.length) {
            if(!args[idx].startsWith("--")) {
            }
        
            // option to process
            if(idx+1 == args.length) {
                System.err.println("No value passed for argument " + args[idx]);
                System.exit(-1);
            }

            switch (args[idx]) {
                case "-d":
                case "--directory":
                    classPath = args[idx+1];
                    break;
                case "-c":
                case "--main-fqnc":
                    mainClassName = args[idx+1];
                    break;
                case "-l":
                case "--level":
                    try {
                        level = Integer.parseInt(args[idx+1]);
                    } catch(NumberFormatException e) {
                        System.err.println("Argument to the " + args[idx] + " option must be an integer.");
                        System.exit(-1);
                    }
                    if(level < 1 || level > 2) {
                        System.err.println("Invalid analysis level! Valid range is {1, 2} only.");
                        System.exit(-1);
                    }
                    break;
                default:
                    System.err.println("Invalid argument option " + args[idx]);
                    System.exit(-1);
                    break;
            }

            idx += 2;
            
        }
    }

    public String getClassPath() { return classPath; }
    public String getMainClassName() { return mainClassName; }
    public int getLevel() { return level; }

}
