package com.qanpi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

class Runner {
    static private Process currentProcess;

    public static void run(File f) {
        try {
            if (PathFinder.getJDK() == null) {
                Console.io.printerr("Couldn't compile because the path to JDK is undefined.");
                return;
            }
            File compiled = compile(f);
            if (compiled != null) execute(compiled);
        } catch (IOException | InterruptedException e) {
            Console.io.printerr("Failed to execute code.");
            e.printStackTrace();
        }
    }

    private static File compile(File f) {
        String[] command = {PathFinder.getJavacPath(), f.getAbsolutePath()};
        ProcessBuilder pb = new ProcessBuilder(command);

        try {
            Process pro = pb.start();
            readErrorStream(pro.getErrorStream());
            int returnVal = pro.waitFor(); //block the chain until the file is compiled so that the old version of the file is not executed by the next method
            if (returnVal == 0)
                return new File(f.getParentFile() + "/" + f.getName().replace(".java", ".class"));
        } catch (IOException | InterruptedException e) {
            Console.io.printerr("Failed to complete the compilation process.");
            e.printStackTrace();
        }
        return null;
    }

    private static void execute(File f) throws IOException, InterruptedException {
        File classPath = new File(f.getPath().replace(".class", "")); //path to the .class file folder
        StringBuilder packagePath = new StringBuilder(); //e.g. com.company.Class

        //go up until in the file structure until "src" folder
        while (!classPath.getName().equals("src")) { //assumed to be eventually returned, since it was checked to be true when opening the file
            packagePath.insert(0, classPath.getName() + ".");
            classPath = classPath.getParentFile();
        }
        packagePath = new StringBuilder(packagePath.substring(0, packagePath.length() - 1)); //remove the extra "." at the end of the package path

        String[] command = {PathFinder.getJavaPath(), "-cp", classPath.getCanonicalPath(), packagePath.toString()};
        ProcessBuilder pb = new ProcessBuilder(command);

        Console.io.println(String.join(" ", command));
        Console.io.println(""); //extra new line
        Process pro = pb.start();
        currentProcess = pro;

        //the error stream will always be printed after all of the input stream,
        //but that's also how IntelliJ works as far as I can tell
        //altho this chunk of code could use some refactoring
        CompletableFuture.runAsync(()-> readInputStream(pro))
                .thenRun(() -> readErrorStream(pro.getErrorStream()));
        CompletableFuture.runAsync(() -> openOutputStream(pro.getOutputStream()));

        pro.waitFor(); //blocks async chain until process finishes naturally or is stopped manually
    }

    private static void readErrorStream(InputStream is) {
        try (Scanner sc = new Scanner(is)) {
            while (sc.hasNextLine()) {
                Console.io.printerr(sc.nextLine());
            }
        }
    }

    private static void readInputStream(Process pro) {
        InputStream is = pro.getInputStream();
        try (Scanner sc = new Scanner(is)) {
            while (sc.hasNextLine() && currentProcess != null) { //prevent from running after the process was terminated
                Console.io.println(sc.nextLine());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void openOutputStream(OutputStream outputStream) {
        Console.io.routeTo(outputStream);
    }

    static void finish() {
        if (currentProcess == null) return; //safety in case the process was never started
        currentProcess.descendants().forEach(ProcessHandle::destroy);
        currentProcess.destroy();

        try {
            int exitVal = currentProcess.waitFor();
            currentProcess = null;
            Console.io.println(Console.NEWLINE + "Process finished with exit code " + exitVal);
        } catch (InterruptedException e) {
            Console.io.printerr("An interruption occurred while attempting to finish the process.");
            e.printStackTrace();
        }
    }
}


