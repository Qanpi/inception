package com.qanpi;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

class RunConfiguration {
    private File jdkPath;
    private final String EXT;
    private final OS OP_SYSTEM;

    RunConfiguration(OS os) {
        OP_SYSTEM = os;
        EXT = (OP_SYSTEM == OS.Windows) ? ".exe" : "";
        searchForJDK();
    }

    private void searchForJDK() {
        final String[] commonPaths = {System.getProperty("user.home") + "/.jdks/",
                "C:/Program Files/Java/",
                "C:/Program Files (x86)/Java/"};


        for (String path : commonPaths) {
            File baseDir = new File(path);
            //exit if not a directory
            if(!baseDir.isDirectory() || baseDir.listFiles() == null) continue;

            File[] jdks = baseDir.listFiles(File::isDirectory);
            for (int i=0; i < jdks.length; i++) {
                File jdk = jdks[i];
                File java = new File(jdk + "/bin/java" + EXT), javac = new File(jdk + "/bin/javac" + EXT);

                if (java.exists() && javac.exists()) jdkPath = jdk;
            }
        }
    }

    private void manualPathToJDK() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Please provide a path to the JDK folder");

        int returnVal = fc.showOpenDialog(null);
        while (returnVal == JFileChooser.APPROVE_OPTION) {
            File dir = fc.getSelectedFile();
            File java = new File(dir + "/bin/java" + EXT), javac = new File(dir + "/bin/javac" + EXT);
            if (java.exists() && javac.exists()) jdkPath = dir;
            else {
                JOptionPane.showMessageDialog(fc, "Please provide a path to the JDK folder which contains a 'bin' directory with java and javac executables.",
                        "Invalid path",
                        JOptionPane.ERROR_MESSAGE);
                returnVal = fc.showOpenDialog(null);
            }
        }
    }

    public void run(File f) throws IOException, InterruptedException {
        //TODO: replace this with project structure
        if(f == null) {
            Console.logErr("No file is currently open.");
            return;
        }

        if (jdkPath == null) {
            JOptionPane.showMessageDialog(null, "Please provide the path to the JDK folder manually.",
                    "Path not found", JOptionPane.ERROR_MESSAGE);
            manualPathToJDK();
            return;
        }

        File compiledFile = compile(f);
        execute(compiledFile);
        Console.log("test2");
    }

    private File compile(File f) throws IOException, InterruptedException {
        File javac = new File(jdkPath + "/bin/javac" + EXT);
        ProcessBuilder pb = new ProcessBuilder(javac.getAbsolutePath(), f.getAbsolutePath());

        Process pro = pb.start();
        pro.waitFor(); //wait until the process terminates

        return new File(f.getParentFile() + "/" + f.getName().replace(".java", ".class")); //TODO: clarify this
    }

    private void execute(File f) throws InterruptedException, IOException {
        File java = new File(jdkPath + "/bin/java");
        //TODO: maybe split into separate methods
        File classPath = new File(f.getPath().replace(".class", "")); //path to the .class file folder
        String packagePath = ""; //e.g. com.company.Class

        //go up until in the file structure until "src" folder
        while(!classPath.getName().equals("src")) {
            if (classPath.getParentFile() != null) {
                packagePath = classPath.getName() + "." + packagePath;
                classPath = classPath.getParentFile();
            } else {
                Console.logErr("Please place your .java file in a 'src/' directory");
                return;
            }
        };
        packagePath = packagePath.substring(0, packagePath.length()-1); //remove the extra "." at the end of the package path

        ProcessBuilder pb = new ProcessBuilder(java.getAbsolutePath(), "-cp",  classPath.getAbsolutePath(), packagePath);
        Process pro = pb.start();

        ExecutorService codeRunner = Executors.newSingleThreadExecutor();

        Supplier<String> getOutput = () -> {
            try(Scanner sc = new Scanner(pro.getInputStream())) {
                while (sc.hasNextLine()) {
                    Console.log(sc.nextLine());
                }
            } catch (Exception e) {
                Console.logErr("Error while trying to read from the compiled program.");
                e.printStackTrace();
            }
            return null;
        };

        CompletableFuture<String> future = CompletableFuture.supplyAsync(getOutput, codeRunner);
        future.thenAccept(string -> Console.log(string));
        Console.log("test");

//        pro.waitFor();
    }

    private
}


