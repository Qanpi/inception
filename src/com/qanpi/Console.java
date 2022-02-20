package com.qanpi;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
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
}
