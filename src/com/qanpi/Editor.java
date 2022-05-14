package com.qanpi;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

public class Editor {
    private File currentFile;
    final private JTextPane component;

    File getCurrentFile() {
        return currentFile;
    }

    JTextPane getComponent() {
        return component;
    }

    Editor () {
        component = new JTextPane();
        updateFilter();
    }

    void openFile(File f) throws IOException {
        component.setPage(f.toURI().toURL());
        updateFilter();
        currentFile = f;
    }

//    void setTabSpacing() {
//        TabStop[] tabs = new TabStop[1];
//        tabs[0] = new TabStop(150, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE);
//        TabSet tabset = new TabSet(tabs);
//
//        SimpleAttributeSet sas = new SimpleAttributeSet();
//        StyleConstants.setTabSet(sas, tabset);
//        StyleConstants.setForeground(sas, Color.red);
//
//        component.setParagraphAttributes(sas, true);
//        component.setCharacterAttributes(sas, false);
//
//        System.out.println(component.getParagraphAttributes().getAttributeNames().nextElement());
//    }

    String getText() {
        return component.getText();
    }

    private void updateFilter() {
        Document doc = component.getDocument();
        ((AbstractDocument) doc).setDocumentFilter(new EditorFilter());
    }

}

class EditorFilter extends DocumentFilter {
    final int TAB_SIZE = 4;

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        super.insertString(fb, offset, string.replace("\t", "    "), attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        if (text == null) text = ""; //to prevent errors with .setText(null) as pointed out by SOF

        if(text.equals("\t")) {
            String pretext = fb.getDocument().getText(0, offset);
            int lineStart = pretext.lastIndexOf('\n') + 1;

            int filling = TAB_SIZE - (offset - lineStart) % TAB_SIZE; //find the number of characters until the tab column
            super.replace(fb, offset, length, text.replace("\t", " ".repeat(filling)), attrs);
            return;
        }
        super.replace(fb, offset, length, text.replace("\t", "    "), attrs);
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {

        if (length == 1) { //assuming the backspace is pressed, but also triggered by delete key :(
            String pretext = fb.getDocument().getText(0, offset);
            int lineStart = pretext.lastIndexOf('\n') + 1;
            int rem = (offset - lineStart) % TAB_SIZE; //find the number of characters over the tab column

            String text = fb.getDocument().getText(offset - rem, rem + 1);
            if (text.isBlank()) {
                super.remove(fb, offset - rem, rem + 1);
                return;
            }
        }

        super.remove(fb, offset, length);
    }
}
