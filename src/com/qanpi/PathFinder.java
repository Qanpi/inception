package com.qanpi;

import javax.swing.*;
import java.io.File;

class PathFinder {
    static private File JDK;

    static File getJDK() {
        if (JDK == null) JDK = searchForJDK();
        if (JDK == null) JDK = manualPathToJDK();
        return JDK; //returns null if the previous failed!
    }

    static String getJavaPath() {
        File java = new File(JDK + "/bin/java");
        return java.getAbsolutePath();
    }

    static String getJavacPath() {
        File javac = new File(JDK + "/bin/javac" + PathFinder.getExtension());
        return javac.getAbsolutePath();
    }

    static private String getExtension() {
        String osName = System.getProperty("os.name");
        if(osName.startsWith("Windows")) return ".exe";
        else if (osName.startsWith("Linux")) return "";
        else throw new RuntimeException("Unsupported operating system.");
    }


    static private File searchForJDK() {
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
                File java = new File(jdk + "/bin/java" + PathFinder.getExtension()),
                        javac = new File(jdk + "/bin/javac" + PathFinder.getExtension());

                if (java.exists() && javac.exists()) return jdk;
            }
        }
        return null;
    }

    private static File manualPathToJDK() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setFileHidingEnabled(false);
        fc.setDialogTitle("Please provide a path to the JDK folder");

        //Loop until valid jdk path entered
        int returnVal = fc.showOpenDialog(null);
        while (returnVal == JFileChooser.APPROVE_OPTION) {
            File dir = fc.getSelectedFile();
            File java = new File(dir + "/bin/java" + PathFinder.getExtension());
            File javac = new File(dir + "/bin/javac" + PathFinder.getExtension());

            if (java.exists() && javac.exists()) return dir;
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
