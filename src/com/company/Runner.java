package com.company;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;

/*
& "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2021.2.3\jbr\bin\javac.exe" .\Main.java
& "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2021.2.3\jbr\bin\java.exe" -classpath "C:\Users\aleks\OneDrive - Suomalaisen Yhteiskoulun Osakeyhtiö\Tiedostot\School\Pre-IB\Term 3\ComSci\Lesson8\out\production\Lesson8" com.company.Main

 */

class Runner {
    private String javaPath;
    private String javacPath;



    void checkCommonJDKPath() {
        //Try to find the location of java development kit automatically
        File jdk = null;

        //Method 1 - checking for the "C:/Users/<user.name>/.jdks/openjdk-17.0.1" directory
        File dir = new File(System.getProperty("user.home") + "/.jdks/");
        String p = dir.getPath().replace("\\", "/");
        String pat = "glob:"+p+"/openjdk*";
        PathMatcher pm = FileSystems.getDefault().getPathMatcher(pat);
        System.out.println(pat);

        //Objects.requireNonNull because the IDE suggested it
        for (File f1 : dir.listFiles()) {
            if(f1.isDirectory()) {
                for (File f2 : f1.listFiles()) {
                    System.out.println(f2);
                    System.out.println(pm.matches(f2.toPath()));
                }
            }
        }
//            System.out.println(Path.of(f1.getAbsolutePath()));
//            if(pm.matches(Path.of(f.getAbsolutePath()))) System.out.println("YAYA" + f.getName());
////            if (f.getName().contains("openjdk") && f.isDirectory()) jdk = f;
        }

//    File usingWhereCommand() {
//        //The following only works if java is included in the Path enviroment variable
//        //Only works if java and javac are included into path
//        try {
//            Process pro = Runtime.getRuntime().exec("where javac.exe");
//            return pro.inputReader().readLine();
//        } catch (IOException e) {
//            System.err.println("An error has occurred while trying to find" + prg);
//            e.printStackTrace();
//        } return null;
//    }


    private String autoFindPath(String prg) {
        String path = null;
        //Try to find the location of java compiler
        try {
            Process pro = Runtime.getRuntime().exec("where " + prg);
            path = pro.inputReader().readLine();
        } catch (IOException e) {
            System.err.println("An error has occurred while trying to find" + prg);
            e.printStackTrace();
            System.exit(1);
        }

        //If where command fails
        if (path == null)  {
            System.err.println("Unable to find path to the program automatically, asking for user input");
            Console.logErr("Unable to find path to " + prg + " automatically. Please select the corresponding file manually.");
            return manualFindPath(prg);
        }
        return path;
    }

    private String manualFindPath(String prg) {

        return "test";
    }
}
