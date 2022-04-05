package com.qanpi;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;

class Console {
    static String newLine = "\n";
    final static private JTextPane component = new JTextPane();

    static JTextPane getComponent() {
        return component;
    }

    static void setEditable(boolean b) {
        component.setEditable(b);
    }

    static private void log(String msg, SimpleAttributeSet sas) {
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

    static String read() {
        return component.getText();
    }

    static void listen(OutputStream os) {
        Action testAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = component.getText();
                String input = text.substring(text.lastIndexOf("\n")+1);
                System.out.println(input);
                PrintWriter pw = new PrintWriter(os, true);
                pw.println(input);
                Console.log("");
            }
        };

        component.registerKeyboardAction(testAction, KeyStroke.getKeyStroke("ENTER"), JComponent.WHEN_FOCUSED);
    }

    static void refocus() {
        KeyListener focusAction = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                component.setCaretPosition(component.getDocument().getLength());
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        };

        component.addKeyListener(focusAction);

    }

    static void clear() {
        component.setText("");
    }

//    class ConsoleInputStream extends InputStream {
//
//        ConsoleInputStream() {
//
//        }
//
//        @Override
//        public int read() throws IOException {
//            try {
//                component.getText();
//            } catch (BadLocationException e) {
//                e.printStackTrace();
//            }
//        }
//    }
}
