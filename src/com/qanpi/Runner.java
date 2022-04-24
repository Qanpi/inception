package com.qanpi;

import javax.swing.*;
import java.io.*;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

class Runner {
    private Process currentProcess;
    final private PathFinder pm;

    Runner () {
        pm = new PathFinder();
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

            //System.out.println(f.getAbsolutePath().replace(".java", ".class"));
            File compiled = new File(f.getParentFile() + "/" + f.getName().replace(".java", ".class"));
            //System.out.println(f.getParentFile() + "/" + f.getName().replace(".java", ".class"));
            return compiled; //TODO: clarify this
    }

    private void execute(File f) throws IOException, InterruptedException {
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

        //the error stream will always be printed after all of the input stream,
        //but that's also how IntelliJ works as far as I can tell
        //altho this chunk of code could use some refactoring
        CompletableFuture.runAsync(()-> readInputStream(pro))
                .thenRun(() -> readErrorStream(pro.getErrorStream()));
        CompletableFuture.runAsync(() -> openOutputStream(pro.getOutputStream()));

        pro.waitFor(); //blocks async chain until process finishes naturally or is stopped manually
    }

    private void readErrorStream(InputStream is) {
        try (Scanner sc = new Scanner(is)) {
            while (sc.hasNextLine()) {
                Console.io.printerr(sc.nextLine());
            }
        }
    }

    private void readInputStream(Process pro) {
        InputStream is = pro.getInputStream();
        try (Scanner sc = new Scanner(is)) {
            while (sc.hasNextLine()) {
                Console.io.println(sc.nextLine());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openOutputStream(OutputStream outputStream) {
        Console.io.routeTo(outputStream);
    }

    void finish() {
        //if (currentProcess == null) return; //safety in case the stop button is pressed right after the process ends naturally
        //i think the above is not needed anymore but i'm not sure lmao
        currentProcess.descendants().forEach(ProcessHandle::destroy);
        currentProcess.destroy();

        Console.io.println(Console.NEWLINE + "Process finished with exit code " + currentProcess.exitValue());
        currentProcess = null;
    }
}


