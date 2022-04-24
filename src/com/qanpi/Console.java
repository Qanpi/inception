package com.qanpi;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.OutputStream;
import java.io.PrintWriter;

class Console {
    final static String NEWLINE = System.lineSeparator();
    static IO io;

    final private JTextPane component; //maybe not going to be final in future

    JTextPane getComponent() {
        return component;
    }

    Console () {
        component = new JTextPane();
        component.setEditable(false);
        bindUserInputListener();

        io = new IO();
        io.bindUserOutputWriter();
    }

    /**
     * Listen to input into the console and automatically redirect it to the end of the already present text.
     */
    void bindUserInputListener() {
        KeyListener focusAction = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (!Console.io.isEditable()) return;

                int last = component.getDocument().getLength();
                component.setCaretPosition(last);
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



    class IO {
        private PrintWriter processWriter;
        private void println(String msg, SimpleAttributeSet sas) {
            try {
                Document doc = component.getDocument();
                doc.insertString(doc.getLength(), msg + NEWLINE, sas);
            } catch (BadLocationException e) {
                //non-recoverable error as there is no way to indicate it to the user if the console is broken
                System.err.println("An unknown error occurred when trying to append text to the document.");
                e.printStackTrace();
                System.exit(2);
            }
        }

        void println(String msg) {
            println(msg, new SimpleAttributeSet());
        }

        void printerr(String msg) {
            //make the text color of exceptions red
            SimpleAttributeSet errorStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(errorStyle, Color.red);
            println(msg, errorStyle);
        }

        void bindUserOutputWriter () {
            KeyListener routeAction = new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {

                }

                @Override
                public void keyPressed(KeyEvent e) {
                    if (!isEditable()) return;

                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        e.consume(); //prevent from the default Enter key behaviour

                        String text = component.getText();
                        String userInput = text.substring(text.lastIndexOf("\n")+1);

                        processWriter.println(userInput);
                        println(""); //print a new line in the user console after the input
                    } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                        e.consume();
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {

                }
            };

            component.addKeyListener(routeAction);
        }

        /**
         * Routs the text entered by the user to a certain OutputStream.
         * @param outputStream The OutputStream to route text to
         */
        void routeTo(OutputStream outputStream) {
            if (processWriter != null) processWriter.close();
            processWriter = new PrintWriter(outputStream, true);
        }

        void clear() {
            component.setText("");
        }

        void setEditable(boolean b) {
            component.setEditable(b);
        }

        public boolean isEditable() {
            return component.isEditable();
        }
    }
}
