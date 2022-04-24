package com.qanpi;

import javax.swing.*;
import java.io.File;

class PathFinder {
    private String EXTENSION;
    private File java;
    private File javac;

    String getJavaPath() {
        return java.getAbsolutePath();
    }

    String getJavacPath() {
        return javac.getAbsolutePath();
    }

    PathFinder () {
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
        if (jdkPath == null) jdkPath = manualPathToJDK();

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
