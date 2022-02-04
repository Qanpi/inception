package com.company;

import javax.swing.filechooser.FileFilter;
import java.io.File;

class JavaExtensionFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) return true;

        int i = f.getName().indexOf(".");
        if (i != -1) {
            String ext = f.getName().substring(i);
            if (ext.equals(".java")) return true;
        }

        return false;
    }

    @Override
    public String getDescription() {
        return ".java";
    }
}

class TextExtensionFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) return true;

        int i = f.getName().indexOf(".");
        if (i != -1) {
            String ext = f.getName().substring(i);
            if (ext.equals(".txt")) return true;
        }

        return false;
    }

    @Override
    public String getDescription() {
        return ".txt";
    }
}
