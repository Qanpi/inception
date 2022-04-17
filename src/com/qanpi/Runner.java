package com.qanpi;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.*;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
//Exception "java.lang.ClassNotFoundException: com/intellij/codeInsight/editorActions/FoldingData"while constructing DataFlavor for: application/x-java-jvm-local-objectref; class=com.intellij.codeInsight.editorActions.FoldingData

class PathMaker {
    private String EXTENSION;
    private File java;
    private File javac;

    String getJavaPath() {
        return java.getAbsolutePath();
    }

    String getJavacPath() {
        return javac.getAbsolutePath();
    }

    PathMaker () {
        setExtension();
        setJavaAndJavac();
    }

    private void setExtension() {
        String osName = System.getProperty("os.name");
        if(osName.startsWith("Windows")) EXTENSION = ".exe";
        else if (osName.startsWith("Linux")) EXTENSION = "";
        else throw new RuntimeException("Unsupported operating system."); //tODO:check what throwing the excpetion does
    }

    private void setJavaAndJavac() {
        File jdkPath = searchForJDK();
        if (jdkPath == null) manualPathToJDK();

        javac = new File(jdkPath + "/bin/javac" + EXTENSION);
        java = new File(jdkPath + "/bin/java");
    }

    private File searchForJDK() {
        final String[] commonPaths = {
                System.getProperty("user.home") + "/.jdks/",
                "C:/Program Files/Java/",
                "C:/Program Files (x86)/Java/"
        };

        for (String path : commonPaths) {
            File baseDir = new File(path);
            //skip if not a directory
            if(!baseDir.isDirectory() || baseDir.listFiles() == null) continue;

            //list all directories (presumably the jdk directories)
            File[] jdks = baseDir.listFiles(File::isDirectory);
            for (int i=0; i < jdks.length; i++) {
                File jdk = jdks[i];
                File java = new File(jdk + "/bin/java" + EXTENSION),
                     javac = new File(jdk + "/bin/javac" + EXTENSION);

                if (java.exists() && javac.exists()) return jdk;
            }
        }
        return null;
    }

    private File manualPathToJDK() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Please provide a path to the JDK folder");

        //Loop until valid jdk path entered
        int returnVal = fc.showOpenDialog(null);
        while (returnVal == JFileChooser.APPROVE_OPTION) {
            File dir = fc.getSelectedFile();
            File java = new File(dir + "/bin/java" + EXTENSION), javac = new File(dir + "/bin/javac" + EXTENSION);

            if (java.exists() && javac.exists())
                return dir;
            else {
                JOptionPane.showMessageDialog(
                        fc,
                        "Please provide a path to the JDK folder which contains a 'bin' directory with java and javac executables.",
                        "Invalid path",
                        JOptionPane.ERROR_MESSAGE);
                returnVal = fc.showOpenDialog(null);
            }
        }
        return null;
    }
}

class Runner {
    private Process currentProcess;
    final private PathMaker pm;

    Runner () {
        pm = new PathMaker();
    }

    public void run(File f) {
        //TODO: replace this with project structure
        if(f == null) {
            Console.io.printerr("No file is currently open.");
            return;
        }

        try {
            File compiled = compile(f);
            execute(compiled);
        } catch (IOException | InterruptedException e) {
            Console.io.printerr("Failed to execute code.");
        }
    }

    private File compile(File f) {
            String[] command = {pm.getJavacPath(), f.getAbsolutePath()};
            ProcessBuilder pb = new ProcessBuilder(command);

            try {
                Process pro = pb.start();
                readErrorStream(pro.getErrorStream());
                pro.waitFor(); //block the chain until the file is compiled so that the old version of the file is not executed by the next method
            } catch (IOException | InterruptedException e) {
                Console.io.printerr("Failed to complete the compilation process.");
                e.printStackTrace();
            }

            System.out.println(f.getAbsolutePath().replace(".java", ".class"));
            File compiled = new File(f.getParentFile() + "/" + f.getName().replace(".java", ".class"));
            System.out.println(f.getParentFile() + "/" + f.getName().replace(".java", ".class"));
            Console.io.println("test");
            return compiled; //TODO: clarify this
    }

    private void execute(@NotNull File f) throws IOException, InterruptedException {
        //TODO: maybe split into separate methods
        File classPath = new File(f.getPath().replace(".class", "")); //path to the .class file folder
        StringBuilder packagePath = new StringBuilder(); //e.g. com.company.Class

        //go up until in the file structure until "src" folder
        while (!classPath.getName().equals("src")) {
            if (classPath.getParentFile() != null) {
                packagePath.insert(0, classPath.getName() + ".");
                classPath = classPath.getParentFile();
            } else {
                Console.io.printerr("Please place your .java file in a 'src/' directory");
            }
        }
        packagePath = new StringBuilder(packagePath.substring(0, packagePath.length() - 1)); //remove the extra "." at the end of the package path

        String[] command = {"\"" + pm.getJavaPath() + "\"", "-cp", "\"" + classPath.getCanonicalPath() + "\"", packagePath.toString()};
        ProcessBuilder pb = new ProcessBuilder(command);

        Console.io.println(String.join(" ", command));
        Process pro = pb.start();
        currentProcess = pro;


        CompletableFuture.runAsync(()-> readInputStream(pro))
                .thenRun(() -> readErrorStream(pro.getErrorStream()));
        CompletableFuture.runAsync(() -> openOutputStream(pro.getOutputStream()));

        pro.waitFor();
    }

    private void readErrorStream(InputStream is) {
        System.out.println("error stream");
        try (Scanner sc = new Scanner(is)) {
            while (sc.hasNextLine()) {
                Console.io.printerr(sc.nextLine());
            }
        }
    }

    private void readInputStream(Process pro) {
        InputStream is = pro.getInputStream();
        System.out.println("input stream");
        try (Scanner sc = new Scanner(is)) {
//            wait(3000);
            while (pro.isAlive()) {
                if (sc.hasNextLine()) Console.io.println(sc.nextLine());
            }
            System.out.println(pro.isAlive());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openOutputStream(OutputStream os) {
        System.out.println("output stream");

//        while (true) System.out.println("test");
        Console.io.routeTo(os);

//        try (Scanner sc = new Scanner(System.in)) {
//            while(sc.hasNextLine()) System.out.println(sc.nextLine());
//        }
    }

    void finish() {
        if (currentProcess == null) return;
        currentProcess.descendants().forEach(ProcessHandle::destroy);
        currentProcess.destroy();
        Console.io.println(Console.NEWLINE + "Process finished with exit code " + currentProcess.exitValue());
        currentProcess = null;
    }

    boolean isRunning() {
        return (currentProcess != null);
    }




}


