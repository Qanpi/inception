package com.qanpi;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.*;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
//Exception "java.lang.ClassNotFoundException: com/intellij/codeInsight/editorActions/FoldingData"while constructing DataFlavor for: application/x-java-jvm-local-objectref; class=com.intellij.codeInsight.editorActions.FoldingData

//TODO: refactor, A LOT
class Runner {
    private final OS OS;
    private final String EXT;
    private File jdkPath;
    private Process currentProcess;

    enum OS {
        Windows,
        Linux
    }

    Runner () {
        OS = getOS();
        EXT = (OS == OS.Windows) ? ".exe" : "";
        searchForJDK();
    }

    private OS getOS() {
        //Determine and store the OS of the user
        String osName = System.getProperty("os.name");
        if(osName.startsWith("Windows")) return OS.Windows;
        else if (osName.startsWith("Linux")) return OS.Linux;
        else throw new RuntimeException("Unsupported operating system.");
    }

    public void run(File f) {
        //TODO: replace this with project structure
        if(f == null) {
            Console.io.printerr("No file is currently open.");
            return;
        }

        if (jdkPath == null) {
            JOptionPane.showMessageDialog(null, "Please provide the path to the JDK folder manually.",
                    "Path not found", JOptionPane.ERROR_MESSAGE);
            manualPathToJDK();
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
            File javac = new File(jdkPath + "/bin/javac" + EXT);
            String[] command = {javac.getAbsolutePath(), f.getAbsolutePath()};
            ProcessBuilder pb = new ProcessBuilder(command);

            try {
                Process pro = pb.start();
                readErrorStream(pro.getErrorStream());
                if (pro.exitValue() != 0) return null; //prevents the async chain from being executed if the compilation didn't succeed
            } catch (IOException e) {
                Console.io.printerr("Failed to start the compilation process.");
                e.printStackTrace();
            }
            return new File(f.getParentFile() + "/" + f.getName().replace(".java", ".class")); //TODO: clarify this
    }

    private void execute(@NotNull File f) throws IOException, InterruptedException {
        File java = new File(jdkPath + "/bin/java");
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

        String[] command = {"\"" + java.getCanonicalPath() + "\"", "-cp", "\"" + classPath.getCanonicalPath() + "\"", packagePath.toString()};
        ProcessBuilder pb = new ProcessBuilder(command);

        Console.io.println(String.join(" ", command));
        Process pro = pb.start();
        currentProcess = pro;
        //technically this means that the error and input stream will be printed out fully one after the other, but that's also how IntelliJ works
//        CompletableFuture<Void> readInput = CompletableFuture.runAsync(()-> readInputStream(pro.getInputStream()));
//        CompletableFuture<Void> readError = CompletableFuture.runAsync(()-> readErrorStream(pro.getErrorStream()));
//        CompletableFuture<Void> writeInput = CompletableFuture.runAsync(() -> openOutputStream(pro.getOutputStream()));

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
        Console.io.println(Console.nL + "Process finished with exit code " + currentProcess.exitValue());
        currentProcess = null;
    }

    boolean isRunning() {
        return (currentProcess != null);
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
}


