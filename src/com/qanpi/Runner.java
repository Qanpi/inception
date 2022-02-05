package com.qanpi;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

class Runner {
    private File jdkPath;

    Runner () {
        //Try the first automatic method
        jdkPath = searchForJDK();

        //Try the second automatic method
        if(jdkPath == null) jdkPath = searchUsingWhere();
    }

    public void run(File f) {
        if(f == null) {
            Console.logErr("No file is currently open.");
            return;
        } else if (jdkPath == null) {
            jdkPath = manualPathToJDK();
            return;
        }

        File compiledFile = compile(f);
        execute(compiledFile);
    }

    private File compile(File f) {
        try {
            File javac = new File(jdkPath + "/javac.exe");
            String command = String.format("\"%s\" \"%s\"", javac, f);
            System.out.println(command);

            Process pro = Runtime.getRuntime().exec(command);
            pro.waitFor(); //wait until the process terminates

            File classFile = new File(f.getParentFile() + "/" + f.getName().replace(".java", ".class"));
            return classFile;
        } catch (IOException | InterruptedException e) {
            Console.logErr("Unable to compile the file. Process interrupted. ");
            e.printStackTrace();
        }
        return null;
    }

    private void execute(File compiled) {
        try {
            File java = new File(jdkPath + "/java.exe");
            File classPath = new File(compiled.getPath().replace(".class", ""));
            String filePath = "";

            //go up until in the file structure until "src" folder
            while(!classPath.getName().equals("src")) {
                if (classPath.getParentFile() != null) {
                    filePath = classPath.getName() + "." + filePath;
                    classPath = classPath.getParentFile();
                }
                else {
                    Console.logErr("Please place your .java file in a 'src/' directory");
                    return;
                }
            };
            filePath = filePath.substring(0, filePath.length()-1); //dirty fix to remove the extra "." at the end

            String command = String.format("\"%s\" -cp \"%s\" %s", java, classPath, filePath);
            System.out.println(command);

            Process pro = Runtime.getRuntime().exec(command);

            try(Scanner sc = new Scanner(pro.getInputStream())) {
                while (sc.hasNextLine()) {
                    Console.log(sc.nextLine());
                }
            } catch (Exception e) {
                Console.logErr("Error while trying to read from the compiled program.");
                e.printStackTrace();
            }

            pro.waitFor();

        } catch (IOException | InterruptedException e) {
            Console.logErr("Unable to execute the compiled file.");
            e.printStackTrace();
        }
    }

    private File manualPathToJDK() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Please a provide a path to the JDK folder");

        int returnVal = -1;
        while (returnVal == -1 || returnVal == JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(fc, "Please provide the path to the JDK/bin folder manually.",
                    "The program was not able to locate the JDK.", JOptionPane.ERROR_MESSAGE);
            returnVal = fc.showOpenDialog(null);

            File dir = fc.getSelectedFile();
            if (new File(dir + "/java.exe").exists()
                    &&  new File(dir + "/javac.exe").exists()) return dir;
        }
        return null;
    }

    private File searchForJDK() {
        String[] commonPaths = {System.getProperty("user.home") + "/.jdks/",
                                "C:/Program Files/Java/",
                                "C:/Program Files (x86)/Java/"};

        for (String path : commonPaths) {
            File baseDir = new File(path);
            for (File f1 : baseDir.listFiles()) {
                if (f1.isDirectory()) { //check in all directories
                    File jdkBin = new File(f1.getPath() + "/bin/");
                    File java = new File(jdkBin + "/java.exe"), javac = new File(jdkBin + "/javac.exe");

                    if (java.exists() && javac.exists()) return jdkBin;
                }
            }
        }
        return null;
    }

    private File searchUsingWhere() {
        //The following only works if java is included in the Path enviroment variable
        try {
            //Very scary looking code
            Process java = Runtime.getRuntime().exec("where.exe java.exe"); //runs console command "where.exe java.exe"
            HashSet<String> javaPaths = new HashSet<>(java.inputReader().lines().map(
                    s -> s.replace("java.exe", "")
            ).toList()); //gets output from the command and parses the paths;
            // stores in a hashset so that duplicates can later be identified, since multiple paths can be present for java.exe

            Process javac = Runtime.getRuntime().exec("where.exe javac.exe"); //runs console command "where.exe javac.exe"
            ArrayList<String> javacPaths = new ArrayList<>(javac.inputReader().lines().map(
                    s -> s.replace("javac.exe", "")
            ).toList()); //gets output from the command and parses the paths;
            //stores in an arraylist to iterate over the elements and attempt to add them to the hashset, thus finding duplicates.

            //returns the duplicates from two paths lists, if present
            for (int i=0; i<javacPaths.size(); i++) {
                if (!javaPaths.add(javacPaths.get(i))) {
                    return new File(javacPaths.get(i));
                }
            }
        } catch (Exception e) {
            System.err.println("An error has occurred while trying to search for JDK path using where.exe");
            e.printStackTrace();
        } return null;
    }


}
