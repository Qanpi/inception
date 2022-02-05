import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

public class Inception extends JFrame {
    private JTextPane editor;
    private Console console;
    private Runner runner;
    private File currentFile;

    Inception() {
        super("Inception");
System.out.println("adding a new line...");
        //Create the main editor text pane
        editor = new JTextPane();
        //and the scroll pane for it
        JScrollPane editorScrollPane = new JScrollPane(editor);
        editorScrollPane.setPreferredSize(new Dimension(600, 300));

        //Create the console uneditable text pane
        //and the scroll pane for it
        console = new Console();
        JScrollPane consoleScrollPane = new JScrollPane(console.getComponent());
        consoleScrollPane.setPreferredSize(new Dimension(300, 200));


        //Split pane between editor and console - allows the panes to be resized
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScrollPane, consoleScrollPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(1); //so that the console doesn't shrink with the window

        add(splitPane, BorderLayout.CENTER);

        //Create the menu bar at the top of the screen
        setJMenuBar(createMenuBar());
    }

    private JMenuBar createMenuBar() {
        JMenuBar mb = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new OpenFileAction());
        fileMenu.add(new SaveFileAction());

        JMenu codeMenu = new JMenu("Code");
        codeMenu.add(new RunAction());

        mb.add(fileMenu);
        mb.add(codeMenu);

        return mb;
    }

    private void updateCurrentFile(File f) {
        currentFile = f;
        setTitle(currentFile.getName());
    }


    class OpenFileAction extends AbstractAction {
        public OpenFileAction() {
            super("Open...");
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            String openPath = currentFile == null ? System.getProperty("user.dir") : currentFile.getPath();
            JFileChooser fc = new JFileChooser(openPath); //open current directory initially
            fc.setFileFilter(new JavaExtensionFilter());
            fc.setAcceptAllFileFilterUsed(false);

            int returnVal = fc.showOpenDialog(null);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                try {
                    editor.setPage(f.toURI().toURL());
                    updateCurrentFile(f);
                } catch (FileNotFoundException e) {
                    Console.logErr("Requested file not found.");
                    e.printStackTrace();
                } catch (IOException e) {
                    Console.logErr("The contents of the file are not supported by the editor.");
                    e.printStackTrace();
                }
            }
        }
    }

    class SaveFileAction extends AbstractAction {
        public SaveFileAction() {
            super("Save As...");
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            String openPath = currentFile == null ? System.getProperty("user.dir") : currentFile.getPath();
            JFileChooser fc = new JFileChooser(openPath); //open current file location
            fc.setFileFilter(new JavaExtensionFilter());
            fc.setAcceptAllFileFilterUsed(false);

            int returnVal = fc.showSaveDialog(null);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                String ext = fc.getFileFilter().getDescription();
                File f = fc.getSelectedFile();

                if (!f.getName().contains(ext)) //in case the file name is given as just "file" (with no .ext)
                    f = new File(fc.getSelectedFile() + ext);

                try (FileWriter fw = new FileWriter(f)) {
                    fw.write(editor.getText());
                } catch (IOException e) {
                    Console.logErr("Unable to save the editor contents to the provided file.");
                    e.printStackTrace();
                }
                updateCurrentFile(f);
            }
        }
    }

    class RunAction extends AbstractAction {
        RunAction() {
            super("Run");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (runner == null) runner = new Runner();
            runner.run(currentFile);
        }
    }


    public static void main(String[] args) {
        //I just copy-pasted the following from an example...

        // Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(() -> {
            //Turn off metal's use of bold fonts
            UIManager.put("swing.boldMetal", Boolean.FALSE);
            createAndShowGUI();
        });
    }

    //Open the editor's GUI at startup
    private static void createAndShowGUI() {
        final JFrame IDE = new Inception();
        IDE.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        IDE.pack();
        IDE.setVisible(true);
    }
}

class Runner {
    private File jdkPath;

    Runner () {
        //Try the first automatic method
        jdkPath = searchForJDK();

        //Try the second automatic method
        if(jdkPath == null) jdkPath = searchUsingWhere();
    }

    public void run(File f) {
        if(f == null) {
            Console.logErr("No file is currently open.");
            return;
        } else if (jdkPath == null) {
            jdkPath = manualPathToJDK();
            return;
        }

        File compiledFile = compile(f);
        execute(compiledFile);
    }

    private File compile(File f) {
        try {
            File javac = new File(jdkPath + "/javac.exe");
            String command = String.format("\"%s\" \"%s\"", javac, f);
            System.out.println(command);

            Process pro = Runtime.getRuntime().exec(command);
            pro.waitFor(); //wait until the process terminates

            File classFile = new File(f.getParentFile() + "/" + f.getName().replace(".java", ".class"));
            return classFile;
        } catch (IOException | InterruptedException e) {
            Console.logErr("Unable to compile the file. Process interrupted. ");
            e.printStackTrace();
        }
        return null;
    }

    private void execute(File compiled) {
        try {
            File java = new File(jdkPath + "/java.exe");
            File classPath = new File(compiled.getPath().replace(".class", ""));
            String filePath = "";

            //go up until in the file structure until "src" folder
            while(!classPath.getName().equals("src")) {
                if (classPath.getParentFile() != null) {
                    filePath = classPath.getName() + "." + filePath;
                    classPath = classPath.getParentFile();
                }
                else {
                    Console.logErr("Please place your .java file in a 'src/' directory");
                    return;
                }
            };
            filePath = filePath.substring(0, filePath.length()-1); //dirty fix to remove the extra "." at the end

            String command = String.format("\"%s\" -cp \"%s\" %s", java, classPath, filePath);
            System.out.println(command);

            Process pro = Runtime.getRuntime().exec(command);

            try(Scanner sc = new Scanner(pro.getInputStream())) {
                while (sc.hasNextLine()) {
                    Console.log(sc.nextLine());
                }
            } catch (Exception e) {
                Console.logErr("Error while trying to read from the compiled program.");
                e.printStackTrace();
            }

            pro.waitFor();

        } catch (IOException | InterruptedException e) {
            Console.logErr("Unable to execute the compiled file.");
            e.printStackTrace();
        }
    }

    private File manualPathToJDK() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Please a provide a path to the JDK folder");

        int returnVal = -1;
        while (returnVal == -1 || returnVal == JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(fc, "Please provide the path to the JDK/bin folder manually.",
                    "The program was not able to locate the JDK.", JOptionPane.ERROR_MESSAGE);
            returnVal = fc.showOpenDialog(null);

            File dir = fc.getSelectedFile();
            if (new File(dir + "/java.exe").exists()
                    &&  new File(dir + "/javac.exe").exists()) return dir;
        }
        return null;
    }

    private File searchForJDK() {
        String[] commonPaths = {System.getProperty("user.home") + "/.jdks/",
                "C:/Program Files/Java/",
                "C:/Program Files (x86)/Java/"};

        for (String path : commonPaths) {
            File baseDir = new File(path);
            for (File f1 : baseDir.listFiles()) {
                if (f1.isDirectory()) { //check in all directories
                    File jdkBin = new File(f1.getPath() + "/bin/");
                    File java = new File(jdkBin + "/java.exe"), javac = new File(jdkBin + "/javac.exe");

                    if (java.exists() && javac.exists()) return jdkBin;
                }
            }
        }
        return null;
    }

    private File searchUsingWhere() {
        //The following only works if java is included in the Path enviroment variable
        try {
            //Very scary looking code
            Process java = Runtime.getRuntime().exec("where.exe java.exe"); //runs console command "where.exe java.exe"
            HashSet<String> javaPaths = new HashSet<>(java.inputReader().lines().map(
                    s -> s.replace("java.exe", "")
            ).toList()); //gets output from the command and parses the paths;
            // stores in a hashset so that duplicates can later be identified, since multiple paths can be present for java.exe

            Process javac = Runtime.getRuntime().exec("where.exe javac.exe"); //runs console command "where.exe javac.exe"
            ArrayList<String> javacPaths = new ArrayList<>(javac.inputReader().lines().map(
                    s -> s.replace("javac.exe", "")
            ).toList()); //gets output from the command and parses the paths;
            //stores in an arraylist to iterate over the elements and attempt to add them to the hashset, thus finding duplicates.

            //returns the duplicates from two paths lists, if present
            for (int i=0; i<javacPaths.size(); i++) {
                if (!javaPaths.add(javacPaths.get(i))) {
                    return new File(javacPaths.get(i));
                }
            }
        } catch (Exception e) {
            System.err.println("An error has occurred while trying to search for JDK path using where.exe");
            e.printStackTrace();
        } return null;
    }
}

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




