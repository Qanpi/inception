package com.qanpi;

import javax.swing.*;
import javax.swing.text.*;
import java.io.File;
import java.io.IOException;

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

    String getText() {
        return component.getText();
    }

    private void updateFilter() {
        Document doc = component.getDocument();
        ((AbstractDocument) doc).setDocumentFilter(new EditorFilter());
    }

}

class EditorFilter extends DocumentFilter {
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        super.insertString(fb, offset, string.replace("\t", "    "), attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        if (text == null) text = ""; //to prevent errors with .setText(null) as pointed out by SOF
        super.replace(fb, offset, length, text.replace("\t", "    "), attrs);
    }
}
