package com.qanpi;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

class Console {
    static String newLine = "\n";
    static private JTextPane component;

    Console () {
        component = new JTextPane();
        component.setEditable(false);
    }

    JTextPane getComponent() {
        return component;
    }

    static private void log(String msg, SimpleAttributeSet sas) {
        if (component == null) throw new NullPointerException("The console component has not been initialized.");

        try {
            Document doc = component.getDocument();
            doc.insertString(doc.getLength(), msg + newLine, sas);
        } catch (BadLocationException e) {
            System.err.println("An unknown error occurred when trying to append text to the document.");
            e.printStackTrace();
            System.exit(2);
        }
    }

    static void log(String msg) {
        log(msg, new SimpleAttributeSet());
    }

    static void logErr(String msg) {
        //make the text color of exceptions red
        SimpleAttributeSet errorStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(errorStyle, Color.red);
        log(msg, errorStyle);
    }

    static void clear() {
        try {
            Document doc = component.getDocument();
            doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
            Console.logErr("Failed to clear the console.");
        }
    }
}
