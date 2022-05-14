import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;
import javax.swing.text.*;
import java.util.Scanner;

public class Inception extends JFrame implements WindowListener {
    private final Editor editor;
    private final Runner runner;
    private JMenuItem runButton;
    private CompletableFuture<Void> process;

    Inception() {
        super("Inception - No file open");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        //Initialize the editor
        editor = new Editor();
        JScrollPane editorScrollPane = new JScrollPane(editor.getComponent());
        editorScrollPane.setPreferredSize(new Dimension(600, 300));

        //Initialize the console
        Console cl = new Console();
        JScrollPane consoleScrollPane = new JScrollPane(cl.getComponent());
        consoleScrollPane.setPreferredSize(new Dimension(300, 200));

        //Split pane between editor and console
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScrollPane, consoleScrollPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(1); //so that the console doesn't shrink with the window

        add(splitPane, BorderLayout.CENTER);
        setJMenuBar(createMenuBar());
        addWindowListener(this);

        runner = new Runner();
        pack();
        setVisible(true);

        welcomeMessage();
    }

    void welcomeMessage() {
        JLabel link = new JLabel("<HTML><U>GitHub.</U></HTML>");
        link.setForeground(Color.blue);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.setToolTipText("https://github.com/Qanpi/inception");
        link.setAlignmentY(0.75F);
        link.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://github.com/Qanpi/inception"));
                } catch (IOException | URISyntaxException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {}
        });

        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setPreferredSize(new Dimension(300, 100));
        textPane.setText("Inception is a small and rugged, but (as I'd like to think) an intuitive and functional Java IDE. \n\n" +
                "It is not meant for projects or actual use, but more so as a sandbox tool. " +
                "Feel free to play around and learn more about the project on ");
        textPane.insertComponent(link);

        JOptionPane.showConfirmDialog(this,
                textPane,
                "Welcome!", JOptionPane.DEFAULT_OPTION);
    }

    private JMenuBar createMenuBar() {
        JMenuBar mb = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new OpenFileAction());
        fileMenu.add(new SaveAction());
        fileMenu.add(new SaveAsAction());

        JMenu codeMenu = new JMenu("Code");
        runButton = new JMenuItem("Stop");
        runButton.setAction(new RunAction());
        runButton.setEnabled(false);
        codeMenu.add(runButton);

        JMenu settingsMenu = new JMenu("Settings");
        JMenu themeMenu = new JMenu("Themes");

        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            themeMenu.add(new SetThemeAction(info.getName()));
        }
        settingsMenu.add(themeMenu);

        mb.add(fileMenu);
        mb.add(codeMenu);
        mb.add(settingsMenu);

        return mb;
    }

    private void loadFile(File f) throws IOException {
        editor.openFile(f);
        runButton.setEnabled(true);
        setTitle("Inception - " + f.getName());
    }

    class OpenFileAction extends AbstractAction {
        public OpenFileAction() {
            super("Open...");
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            File cf = editor.getCurrentFile();

            String firstPath = System.getProperty("user.dir");
            if (cf != null) firstPath = cf.getPath();

            JFileChooser fc = new JFileChooser(firstPath);
            fc.setFileFilter(new JavaExtensionFilter());
            fc.setAcceptAllFileFilterUsed(false);

            int returnVal = fc.showOpenDialog(Inception.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                try {
                    if (!f.getCanonicalPath().contains(File.separatorChar + "src" + File.separatorChar)){ //for future purposes when executing
                        JOptionPane.showMessageDialog(Inception.this,
                                "Please place your .java file inside a folder named 'src'. Sorry for the inconvenience!",
                                "Error!", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    loadFile(f);
                } catch (IOException e) {
                    Console.io.printerr("Error while trying to open file.");
                    e.printStackTrace();
                }
            }
        }
    }

    class SaveAction extends AbstractAction {
        public SaveAction() {super("Save");}

        @Override
        public void actionPerformed(ActionEvent ev) {
            File cf = editor.getCurrentFile();
            if (cf != null && cf.exists()) {
                try (FileWriter fw = new FileWriter(cf)) {
                    fw.write(editor.getText());
                } catch (IOException e) {
                    Console.io.printerr("Unable to save the editor contents to the provided file.");
                    e.printStackTrace();
                }
            } else {
                //redirect to save as, if no file opened (file doesn't exist)
                new SaveAsAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
            }
        }
    }

    class SaveAsAction extends AbstractAction {
        public SaveAsAction() {
            super("Save As...");
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            File cf = editor.getCurrentFile();

            String firstPath = System.getProperty("user.dir");
            if (cf != null) firstPath = cf.getPath();

            JFileChooser fc = new JFileChooser(firstPath);
            fc.setFileFilter(new JavaExtensionFilter());
            fc.setAcceptAllFileFilterUsed(false);

            int returnVal = fc.showSaveDialog(Inception.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                String ext = fc.getFileFilter().getDescription();
                File f = fc.getSelectedFile();

                if (!f.getName().contains(ext)) //in case the file name is given as just "file" (with no .ext)
                    f = new File(fc.getSelectedFile() + ext);

                try (FileWriter fw = new FileWriter(f)) {
                    fw.write(editor.getText());
                } catch (IOException e) {
                    Console.io.printerr("Unable to save the editor contents to the provided file.");
                    e.printStackTrace();
                }

                setTitle(f.getName());
            }
        }
    }

    void setRunButton() {
        runButton.setText("Run");
        runButton.setAction(new RunAction());
    }

    void setStopButton() {
        runButton.setText("Stop");
        runButton.setAction(new StopAction());
    }

    void toggleConsole() {Console.io.setEditable(!Console.io.isEditable());}

    class RunAction extends AbstractAction {
        RunAction() {
            super("Run");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Console.io.clear(); //remove previous output
            setStopButton();
            toggleConsole();

            File currentFile = editor.getCurrentFile();
            process = CompletableFuture.runAsync(() -> runner.run(currentFile));

            process.thenRun(() -> {
                runner.finish();
                setRunButton();
                toggleConsole();
            });
        }
    }

    class StopAction extends AbstractAction {
        StopAction() {
            super("Stop");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            process.complete(null);
        }
    }

    class SetThemeAction extends AbstractAction {
        SetThemeAction (String themeName) {
            super(themeName);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (e.getActionCommand().equals(info.getName())) {
                    try {
                        UIManager.setLookAndFeel(info.getClassName());
                    } catch (ClassNotFoundException | UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException ex) {
                        Console.io.printerr("The selected theme is not supported.");
                        ex.printStackTrace();
                    }
                    break;
                }
            }
            updateUI();
        }
    }

    private void updateUI() {
        SwingUtilities.updateComponentTreeUI(this);
        pack();
    }

    public static void main(String[] args) {
        //I just copy-pasted the following from an example...

        // Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(() -> {
            final JFrame IDE = new Inception();
        });
    }

    @Override
    public void windowOpened(WindowEvent e) {}

    @Override
    public void windowClosing(WindowEvent e) {
        if (process != null && !process.isDone()) {
            int returnVal = JOptionPane.showConfirmDialog(this, "A process is currently running. Do you wish to terminate and exit?");
            if (returnVal == JOptionPane.YES_OPTION) {
                runner.finish();
                dispose();
            }
        } else {
            dispose();
        }
    }

    @Override
    public void windowClosed(WindowEvent e) {}

    @Override
    public void windowIconified(WindowEvent e) {}

    @Override
    public void windowDeiconified(WindowEvent e) {}

    @Override
    public void windowActivated(WindowEvent e) {}

    @Override
    public void windowDeactivated(WindowEvent e) {}
}

class JavaExtensionFilter extends FileFilter {
    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) return true;

        int i = f.getName().indexOf(".");
        if (i != -1) {
            String ext = f.getName().substring(i);
            return ext.equals(".java");
        }
        return false;
    }

    @Override
    public String getDescription() {
        return ".java";
    }
}

class Runner {
    private Process currentProcess;
    final private PathFinder pm;

    Runner () {
        pm = new PathFinder();
    }

    public void run(File f) {
        try {
            File compiled = compile(f);
            if (compiled != null) execute(compiled);
        } catch (IOException | InterruptedException e) {
            Console.io.printerr("Failed to execute code.");
            e.printStackTrace();
        }
    }

    private File compile(File f) {
        String[] command = {pm.getJavacPath(), f.getAbsolutePath()};
        ProcessBuilder pb = new ProcessBuilder(command);

        try {
            Process pro = pb.start();
            readErrorStream(pro.getErrorStream());
            int returnVal = pro.waitFor(); //block the chain until the file is compiled so that the old version of the file is not executed by the next method
            if (returnVal == 0)
                return new File(f.getParentFile() + "/" + f.getName().replace(".java", ".class"));
        } catch (IOException | InterruptedException e) {
            Console.io.printerr("Failed to complete the compilation process.");
            e.printStackTrace();
        }
        return null;
    }

    private void execute(File f) throws IOException, InterruptedException {
        File classPath = new File(f.getPath().replace(".class", "")); //path to the .class file folder
        StringBuilder packagePath = new StringBuilder(); //e.g. com.company.Class

        //go up until in the file structure until "src" folder
        while (!classPath.getName().equals("src")) { //assumed to be eventually returned, since it was checked to be true when opening the file
            packagePath.insert(0, classPath.getName() + ".");
            classPath = classPath.getParentFile();
        }
        packagePath = new StringBuilder(packagePath.substring(0, packagePath.length() - 1)); //remove the extra "." at the end of the package path

        String[] command = {pm.getJavaPath(), "-cp", classPath.getCanonicalPath(), packagePath.toString()};
        ProcessBuilder pb = new ProcessBuilder(command);

        Console.io.println(String.join(" ", command));
        Process pro = pb.start();
        currentProcess = pro;

        //the error stream will always be printed after all of the input stream,
        //but that's also how IntelliJ works as far as I can tell
        //altho this chunk of code could use some refactoring
        CompletableFuture.runAsync(()-> readInputStream(pro))
                .thenRun(() -> readErrorStream(pro.getErrorStream()));
        CompletableFuture.runAsync(() -> openOutputStream(pro.getOutputStream()));

        pro.waitFor(); //blocks async chain until process finishes naturally or is stopped manually
    }

    private void readErrorStream(InputStream is) {
        try (Scanner sc = new Scanner(is)) {
            while (sc.hasNextLine()) {
                Console.io.printerr(sc.nextLine());
            }
        }
    }

    private void readInputStream(Process pro) {
        InputStream is = pro.getInputStream();
        try (Scanner sc = new Scanner(is)) {
            while (sc.hasNextLine()) {
                Console.io.println(sc.nextLine());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openOutputStream(OutputStream outputStream) {
        Console.io.routeTo(outputStream);
    }

    void finish() {
        if (currentProcess == null) return; //safety in case the process was never started
        currentProcess.descendants().forEach(ProcessHandle::destroy);
        currentProcess.destroy();

        try {
            currentProcess.waitFor();
        } catch (InterruptedException e) {
            Console.io.printerr("An interruption occurred while attempting to finish the process.");
            e.printStackTrace();
        }

        Console.io.println(Console.NEWLINE + "Process finished with exit code " + currentProcess.exitValue());
        currentProcess = null;
    }
}


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
                System.getProperty("user.home") + "/.jdks/c",
                "C:/Program Files/Java/c",
                "C:/Program Files (x86)/Java/c"
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

class Editor {
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







