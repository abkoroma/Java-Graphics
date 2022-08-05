import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.KeyStroke;

/**
 * EasyWindow
 * 
 * Consolidates various frame, component, and event listener classes into a single, easy-to-use class.
 * Use the {@link update} method to update the content of the screen with a new BufferedImage and to box up the most recent user input.  This should be called frequently in a loop.
 * Each time you update, the delta-time also updates.  {@link getDeltaTime} to get the number of seconds since the last update.  Useful for making things happen at a constant rate.
 * Then, use methods such as {@link getKey}, {@link getMouseX}, {@link getMouseButton}, and more to read in user input.
 * 
 * @author Branson Beach
 * @version 2.0
 *
 */
public class EasyWindow implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener, WindowFocusListener, WindowListener, ComponentListener{

 /**
  * The RED index in a 3D int array of an image.
  */
 public static final int RED = 0;
 /**
  * The GREEN index in a 3D int array of an image.
  */
 public static final int GREEN = 1;
 /**
  * The BLUE index in a 3D int array of an image.
  */
 public static final int BLUE = 2;
  
 public static int MODE_PACK = 0;
 public static int MODE_STRETCH = 1;
 public static int MODE_SCALE = 2;
 public static int MODE_FREE = 3;
 public static int MODE_PACK_STRETCH = 4;
 public static int MODE_FULLSCREEN = 5;
 public static int MODE_FULLSCREEN_STRETCH = 6;
 

 
 private static int MOUSE_BUTTON_COUNT = 5;
 private static int KEYBOARD_KEY_COUNT = 128;
 private static boolean QUEUED_INPUT = true; // If true, makes EasyWindow more resistant to lag.
 private static int DEFAULT_MODE = MODE_PACK;

 private EasyWindow lastState;
 private JFrame frame;
 private boolean quit;
 private boolean focus;
 private double lastTime;
 private double deltaTime;
 private int mouseX;
 private int mouseY;
 private int mouseWheelDelta;
 private int imageWidth;
 private int imageHeight;
 private int stretchWidth;
 private int stretchHeight;
 private int displayMode;
 private int xBorder;
 private int yBorder;
 
 private boolean firstUpdate;
 private boolean strongQuit;
 private boolean[] mouseButtonsLive;
 private boolean[] mouseButtonsBegin;
 private boolean[] mouseButtonsEnd;
 private boolean[] keysLive;
 private boolean[] keysBegin;
 private boolean[] keysEnd;
 private LinkedList<Boolean>[] keyUpdates;
 private LinkedList<Boolean>[] mouseButtonsUpdates;
 private HashMap<String, ServerSocket> serverSockets;
 private HashMap<String, Socket> externalSockets;
 private HashMap<String, ObjectInputStream> externalSocketInputStreams;
 private HashMap<String, ObjectOutputStream> externalSocketOutputStreams;
 private HashMap<String, LinkedList<Object>> receivedObjects;
 private ArrayList<Clip> soundClips;
 private BufferedImage contents;
 
 private char activeTextSendKey = 13;
 private char activeTextCancelKey = 27;
 private String activeText = "";
 private boolean activeTextOn = false;
 private boolean activeTextSent = false;
 
 /**
  * Quickly and easily makes an EasyWindow
  */
 public EasyWindow()
 {
  this("EasyWindow", true, DEFAULT_MODE);
 }
 
 /**
  * Creates a new EasyWindow with the given name
  * @param windowName The name for the window
  */
 public EasyWindow(String windowName)
 {
  this(windowName, true, DEFAULT_MODE);
 }
 
 /**
  * Creates a new EasyWindow with the given name and starting image
  * @param windowName The name for the window
  * @param image What to initially display in the window
  */
 public EasyWindow(String windowName, BufferedImage image)
 {
  this(windowName, image, true, DEFAULT_MODE);
 }
 
 /**
  * Creates a new EasyWindow with the given name and starting image
  * @param windowName The name for the window
  * @param image What to initially display in the window
  */
 public EasyWindow(String windowName, int[][][] image)
 {
  this(windowName, true, DEFAULT_MODE);
  setImage(image);
 }
 
 /**
  * Creates a new EasyWindow with the given name and other parameters - chances are, you should be using a simpler constructor than this.
  * strongQuit is true in other constructors, and will make the program end abruptly when you close the window.
  * pack is true in the other constructors and guarantees that the window is not stretched larger than your image.
  * Suppresses unchecked warnings where keyUpdates and mouseButtonUpdates arrays are made.
  * @param windowName The name for the window
  * @param strongQuit If set to true, will guarantee the program closes when you hit X.
  * @param displayMode The mode for the window (shrink to fit, stretch, etc.  Refer to constants MODE_...
  */
 @SuppressWarnings("unchecked")
 public EasyWindow(String windowName, boolean strongQuit, int displayMode)
 {  
  //Set up Frame
  frame = new JFrame(windowName);
  this.strongQuit = strongQuit;
  if(strongQuit)
   frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  else
   frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
  frame.addWindowListener(this);
  frame.addComponentListener(this);
  BufferedImage bi = generateDefaultImage();
  contents = bi;
  imageWidth = bi.getWidth();
  imageHeight = bi.getHeight();
  JLabel label = new JLabel(new ImageIcon(bi));
  label.addMouseListener(this);
  label.setHorizontalAlignment(JLabel.LEFT);
  label.setVerticalAlignment(JLabel.TOP);
  label.addMouseMotionListener(this);
  label.addMouseWheelListener(this);
  frame.addKeyListener(this);
  frame.addWindowFocusListener(this);
  
  frame.getContentPane().add(label);
  
  frame.pack();

  frame.setVisible(true);
  
  //Initialize Vital Variables
  lastState = null;
  quit = false;
  focus = true;
  firstUpdate = true;
  setDisplayMode(displayMode);
  this.stretchHeight = 400;
  this.stretchWidth = 600;
  activeTextSent = false;
  
  //Initialize Other Variables
  mouseX = 0;
  mouseY = 0;
  mouseWheelDelta = 0;
  mouseButtonsLive = new boolean[MOUSE_BUTTON_COUNT];
  mouseButtonsBegin = new boolean[MOUSE_BUTTON_COUNT];
  mouseButtonsEnd = new boolean[MOUSE_BUTTON_COUNT];
  mouseButtonsUpdates = new LinkedList[MOUSE_BUTTON_COUNT];
  keyUpdates = new LinkedList[KEYBOARD_KEY_COUNT];
  for(int i = 0; i < mouseButtonsUpdates.length; i++)
   mouseButtonsUpdates[i] = new LinkedList<Boolean>();
  for(int i = 0; i < keyUpdates.length; i++)
   keyUpdates[i] = new LinkedList<Boolean>();
  serverSockets = new HashMap<String, ServerSocket>();
  externalSockets = new HashMap<String, Socket>();
  externalSocketInputStreams = new HashMap<String, ObjectInputStream>();
  externalSocketOutputStreams = new HashMap<String, ObjectOutputStream>();
  receivedObjects = new HashMap<String, LinkedList<Object>>();
  keysLive = new boolean[KEYBOARD_KEY_COUNT];
  keysBegin = new boolean[KEYBOARD_KEY_COUNT];
  keysEnd = new boolean[KEYBOARD_KEY_COUNT];
  soundClips = new ArrayList<Clip>();
  xBorder = 0;
  yBorder = 0;
  
  lastTime = System.nanoTime();
  deltaTime = 0;
  
  //Initialize Last State
  lastState = new EasyWindow(this);
 }
 
 /**
  * Creates a new EasyWindow with the given name and other parameters - chances are, you should be using a simpler constructor than this.
  * strongQuit is true in other constructors, and will make the program end abruptly when you close the window.
  * pack is true in the other constructors and guarantees that the window is not stretched larger than your image.
  * @param windowName The name for the window
  * @param image The BufferedImage to display initially
  * @param strongQuit If set to true, will guarantee the program closes when you hit X.
  * @param displayMode The mode for the window (shrink to fit, stretch, etc.  Refer to constants MODE_...
  */
 public EasyWindow(String windowName, BufferedImage image, boolean strongQuit, int displayMode)
 {
  this(windowName, strongQuit, displayMode);
  setImage(image);
 }
 
 /**
  * FOR INTERNAL USE ONLY - Creates a copy of the current state of the given EasyWindow, except the JFrame is null
  * @param ew The EasyWindow from which to copy the state.
  */
 public EasyWindow(EasyWindow ew)
 {
  frame = null;
  quit = ew.quit;
  focus = ew.focus;
  mouseX = ew.mouseX;
  mouseY = ew.mouseY;
  mouseWheelDelta = ew.mouseWheelDelta;
  mouseButtonsLive = new boolean[MOUSE_BUTTON_COUNT];
  mouseButtonsBegin = new boolean[MOUSE_BUTTON_COUNT];
  mouseButtonsEnd = new boolean[MOUSE_BUTTON_COUNT];
  keysLive = new boolean[KEYBOARD_KEY_COUNT];
  keysBegin = new boolean[KEYBOARD_KEY_COUNT];
  keysEnd = new boolean[KEYBOARD_KEY_COUNT];
  for(int i = 0; i < mouseButtonsLive.length; i++)
  {
   mouseButtonsLive[i] = ew.mouseButtonsLive[i];
   mouseButtonsBegin[i] = false;
   mouseButtonsEnd[i] = false;
  }
  for(int i = 0; i < keysLive.length; i++)
  {
   keysLive[i] = ew.keysLive[i];
   keysBegin[i] = false;
   keysEnd[i] = false;
  }
  if(ew.lastState != null)
  {
   for(int i = 0; i < mouseButtonsLive.length; i++)
   {
    if(mouseButtonsLive[i] && !ew.lastState.mouseButtonsLive[i])
     mouseButtonsBegin[i] = true;
    if(!mouseButtonsLive[i] && ew.lastState.mouseButtonsLive[i])
     mouseButtonsEnd[i] = true;
   }
   for(int i = 0; i < keysLive.length; i++)
   {
    if(keysLive[i] && !ew.lastState.keysLive[i])
     keysBegin[i] = true;
    if(!keysLive[i] && ew.lastState.keysLive[i])
     keysEnd[i] = true;
   }
  }
  activeTextSent = ew.activeTextSent;
  
  double thisTime = System.nanoTime();
  double deltaTimeNano = thisTime - ew.lastTime;
  deltaTime = deltaTimeNano / 1000000000.0;
  lastTime = thisTime;
  ew.lastTime = thisTime;
 }
 
 /**
  * Generates the default image to display in the EasyWindow
  * If EasyWindow is being used incorrectly and the window's content is never updated,
  * then this will appear, informing the user of the error.
  * @return The default image to show in an EasyWindow
  */
 private static BufferedImage generateDefaultImage()
 {
  //Make image and graphics
  BufferedImage defaultImage = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
  Graphics defaultG = defaultImage.getGraphics();
  //Color background
  defaultG.setColor(new Color(190, 255, 210));
  defaultG.fillRect(0, 0, defaultImage.getWidth(), defaultImage.getHeight());
  //Set up fonts
  Font ewFont = new Font("Arial", Font.BOLD, 50);
  Font smallerFont = new Font("Arial", Font.PLAIN, 20);
  Font smallestFont = new Font("Arial", Font.PLAIN, 18);
  //Draw text
  int x = 30;
  int y = 140;
  defaultG.setColor(new Color(0, 80, 60));
  defaultG.setFont(ewFont);
  defaultG.drawString("EasyWindow", 40, 100);
  defaultG.setFont(smallerFont);
  defaultG.drawString("Welcome to", 150, 40);
  defaultG.drawString("Oops!  Something's not quite right!", x, y);
  y += 20;
  defaultG.drawString("You're not calling the non-static: ", x, y);
  y += 20;
  defaultG.drawString("EasyWindow.update method!", x, y);
  y += 20;
  defaultG.drawString("Your code should look like this:", x, y);
  y += 40;
  defaultG.setFont(smallestFont);
  defaultG.drawString("EasyWindow ew = new EasyWindow(\"Title\");", x, y);
  y += 20;
  defaultG.drawString("while(!ew.getQuit()) {", x, y);
  y += 20;
  defaultG.drawString("    //Your code here", x, y);
  y += 20;
  defaultG.drawString("    ew.update(image);", x, y);
  y += 20;
  defaultG.drawString("}", x, y);
  y += 40;
  defaultG.setFont(smallerFont);
  defaultG.drawString("Or maybe your program just crashed first.", x, y);
  y += 20;
  defaultG.drawString("Or is in an infinite loop.", x, y);
  //Clean up and return
  defaultG.dispose();
  return defaultImage;
 }
 
 
 /**
  * Converts a BufferedImage object into a 3D int array
  * 
  * @param bi
  *            The BufferedImage to convert into an array.
  * @return A 3D integer array of the format [x][y][RGB], where x and y are the
  *         coordinate, and RGB is 0, 1, 2 representing the RED, GREEN, and BLUE
  *         channel respectively
  */
 public static int[][][] toArray(BufferedImage bi) {
  bi = removeTransparency(bi, Color.WHITE);
  int array[][][] = new int[bi.getWidth()][bi.getHeight()][3];
  for (int x = 0; x < array.length; x++)
   for (int y = 0; y < array[x].length; y++) {
    Color c = new Color(bi.getRGB(x, y));
    array[x][y][RED] = c.getRed();
    array[x][y][GREEN] = c.getGreen();
    array[x][y][BLUE] = c.getBlue();
   }
  return array;
 }

 /**
  * Converts a 3D int array into a BufferedImage
  * 
  * @param array
  *            A 3D integer array of the format [x][y][RGB], where x and y are
  *            the coordinate, and RGB is 0, 1, 2 representing the RED, GREEN,
  *            and BLUE channel respectively
  * @return A BufferedImage made from the 3D integer array.
  */
 public static BufferedImage toBufferedImage(int[][][] array) {
  BufferedImage bi = new BufferedImage(array.length, array[0].length, BufferedImage.TYPE_INT_RGB);
  for (int x = 0; x < array.length; x++)
   for (int y = 0; y < array[x].length; y++) {
    Color c = new Color(array[x][y][RED], array[x][y][GREEN], array[x][y][BLUE]);
    int color = c.getRGB();
    bi.setRGB(x, y, color);
   }
  return bi;
 }
 
 public static void saveImage(String filename, int[][][] image)
 {
   saveImage(filename, toBufferedImage(image));
 }
 
 public static void saveImage(String filename, BufferedImage image)
 {
   try {
    String[] extension = filename.split("\\.");
    File file = new File(filename);
    ImageIO.write(image, extension[extension.length - 1], file);
} catch (IOException e) {
    System.err.println("Failed to save image: " + filename);
    e.printStackTrace();
}
 }
 
 /**
  * Loads an array image from a file.
  * The first index is the x-position of a pixel
  * The second index is the y-position of a pixel
  * The third index is the RGB of a pixel (0 for Red, 1 for Green, 2 for Blue)
  * @param filename The filename of the image to load.
  * @return The int[][][] read from the file
  */
 public static int[][][] loadArrayImage(String filename)
 {
   return toArray(loadImage(filename));
 }
 
 /**
  * Loads a BufferedImage from a file.
  * @param filename The filename of the image to load.
  * @return The BufferedImage read from the file.
  */
 public static BufferedImage loadImage(String filename)
 {
    BufferedImage bi = null;
    try {
      bi =ImageIO.read(new File(filename));
      } catch (IOException e) {
       System.err.println("Failed to load image: " + filename);
      e.printStackTrace();
    }
    return bi;
 }
 
 /**
  * Loads a series of BufferedImages from files in the format: filename#.ext where the filename is similar for all images, the only difference being the number at the end.
  * @param filename The name of the image files to load.  The number at the end of the filename can be omitted, or of any size.  Results are sorted alphabetically, so you must append low numbers with zeros as the most significant digit.
  * @return An array of BufferedImages, sorted alphabetically by filename.  To guarantee proper ordering, make sure all files have numbers of the same length by appending leading zeros.
  */
 public static BufferedImage[] loadImages(String filename)
 {
    ArrayList<BufferedImage> bi = new ArrayList<BufferedImage>();
    File thisDir = new File(".");
    File[] allFiles = thisDir.listFiles();
    ArrayList<String> filenames = new ArrayList<String>();
    Pattern p = Pattern.compile("^(\\w+)(\\d*)\\.([a-zA-Z]+)$");
    Matcher m = p.matcher(filename);
    if(!m.matches())
     return new BufferedImage[0];
    String fileBaseName = m.group(1);
    String fileExtension = m.group(3);
    p = Pattern.compile("^(" + fileBaseName + ")(\\d*)\\.(" + fileExtension + ")$");
    for(File f : allFiles)
    {
    m = p.matcher(f.getName());
    if(m.matches())
     {
     filenames.add(f.getName());
     }
    }
    Collections.sort(filenames);
    for(String s : filenames)
    {
     try {
       bi.add(ImageIO.read(new File(s)));
       } catch (IOException e) {
        System.err.println("Failed to load image-set: " + filename);
        e.printStackTrace();
      }
    }
  return bi.toArray(new BufferedImage[0]);
 }
 
  /**
  * Deep copies a 3D array.  Useful when using array images.
  *
  * @param original The original image to copy, as a 3D int array.
  * @return The deep copy of the original image, as a 3D int array.
  */
 public static int[][][] copyArrayImage(int[][][] original)
 {
  int[][][] copy = new int[original.length][original[0].length][original[0][0].length];
  for(int i = 0; i < copy.length; i++)
    for(int j = 0; j < copy[0].length; j++)
          for(int k = 0; k < copy[0][0].length; k++)
                copy[i][j][k] = original[i][j][k];
  return copy;
 }
 
 /**
  * The single most-important method of the class.  Updates the window by showing a new image, by updating the time passed since the last update, and by updating the values of all input variables.
  * Contains side-effects and should only be called once in your program (inside a while loop).
  * @param bi The new BufferedImage to display.
  */
 public void update(BufferedImage bi)
 {
  setImage(bi);
  if(QUEUED_INPUT)
  {
  for(int i = 0; i < keyUpdates.length; i++)
  {
   if(keyUpdates[i].size() > 0)
   keysLive[i] = keyUpdates[i].remove();
  }
  }
  lastState = new EasyWindow(this);
  mouseWheelDelta = 0;
  activeTextSent = false;
  firstUpdate = false;
 }

 /**
  * The single most-important method of the class.  Updates the window by showing a new image, by updating the time passed since the last update, and by updating the values of all input variables.
  * @param image The new image to display, formated as an int[][][].
  */
 public void update(int[][][] image)
 {
 update(getImageOfArray(image));
 }
 
 /**
  * Gets how much the mouse wheel just moved.
  * @return Negative values = up.  Positive values = down.  Zero if it didn't move.
  */
 public int getMouseWheel()
 {
  return lastState.mouseWheelDelta;
 }
 
 /**
  * Gets whether or not the given key is pressed.
  * @param c The char representing the key.
  * @return True if th ekey is currently pressed, false otherwise.
  */
 public boolean getKey(char c)
 {
  return getKey(KeyStroke.getKeyStroke(Character.toUpperCase(c), 0).getKeyCode());
 }
 
 /**
  * Gets whether or not the given key was just recently pressed.
  * @param c The char representing the key.
  * @return True if the key was just pressed, false otherwise.
  */
 public boolean getKeyFirst(char c)
 {
  return getKeyFirst(KeyStroke.getKeyStroke(Character.toUpperCase(c), 0).getKeyCode());
 }
 
 /**
  * Gets whether or not the given key was just released.
  * @param c The char representing the key.
  * @return True if the key was just released, false otherwise.
  */
 public boolean getKeyEnd(char c)
 {
  return getKeyEnd(KeyStroke.getKeyStroke(Character.toUpperCase(c), 0).getKeyCode());
 }
 
 /**
  * Gets whether or not the given key is pressed.
  * @param key The int representing the key.
  * @return True if the key is currently pressed, false otherwise.
  */
 public boolean getKey(int key)
 {
  return lastState.keysLive[key];
 }
 
 /**
  * Gets whether or not the given key was just recently pressed.
  * @param key The int representing the key.
  * @return True if the key was just pressed, false otherwise.
  */
 public boolean getKeyFirst(int key)
 {
  return lastState.keysBegin[key];
 }
 
 /**
  * Gets whether or not the given key was just released.
  * @param key The int representing the key.
  * @return True if the key was just released, false otherwise.
  */
 public boolean getKeyEnd(int key)
 {
  return lastState.keysEnd[key];
 }
 
 /**
  * Gets whether or not the specified mouse button is currently pressed.
  * @param button The mouse button to check.  Left mouse button is 1, middle is 2, right is 3, and 4 / 5 may also exist.
  * @return True if the button is pressed, otherwise false.
  */
 public boolean getMouseButton(int button)
 {
  return lastState.mouseButtonsLive[button - 1];//lastState.mouseButtonsLive[button];
 }
 
 /**
  * Gets whether or not the specified mouse button was just recently pressed.
  * @param button The mouse button to check.  Left mouse button is 1, middle is 2, right is 3, and 4 / 5 may also exist.
  * @return True if the button was just pressed, otherwise false.
  */
 public boolean getMouseButtonFirst(int button)
 {
  return lastState.mouseButtonsBegin[button - 1];
 }
 
 /**
  * Gets whether or not the specified mouse button was just released.
  * @param button The mouse button to check.  Left mouse button is 1, middle is 2, right is 3, and 4 / 5 may also exist.
  * @return True if the button was just released, otherwise false.
  */
 public boolean getMouseButtonEnd(int button)
 {
  return lastState.mouseButtonsEnd[button - 1];
 }

 /**
  * Gets the X-coordinate of the mouse.
  * @return The x coordinate of the mouse, relative to the top-left corner of content of the window.
  */
 public int getMouseX()
 {
  double tempXBorder = 0;
  if(displayMode == MODE_FULLSCREEN)
   tempXBorder = xBorder;
  double percent = ((lastState.mouseX * 1.0) - tempXBorder) / (imageWidth - tempXBorder * 2);
  return Math.min(contents.getWidth(), Math.max(0, (int)(percent * contents.getWidth())));
 }
 
 /**
  * Gets the Y-coordinate of the mouse.
  * @return The y coordinate of the mouse, relative to the top-left corner of content of the window.
  */
 public int getMouseY()
 {
  double tempYBorder = 0;
  if(displayMode == MODE_FULLSCREEN)
   tempYBorder = yBorder;
  double percent = ((lastState.mouseY * 1.0) - tempYBorder) / (imageHeight - tempYBorder * 2);
  return Math.min(contents.getHeight(), Math.max(0, (int)(percent * contents.getHeight())));
 }
 
 /**
  * Useful for loops designed to end the program based on interactions with EasyWindow.
  * Contains side-effects and should only be called once in your program (in the condition for a while loop).
  * @return Normally false, but may switch to true based on the result of other methods.
  */
 public boolean getQuit()
 {
  if(strongQuit && lastState.quit)
   frame.dispose();
  return lastState.quit;
 }

 /**
  * Sets the quit variable that you get via the getQuit method.  Hitting the X in the window also sets it to true, but you can change it manually if there's other criteria to your program.
  * @param newQuit The new state of quit.  Normally, you'll only ever be setting it to true.
  */
 public void setQuit(boolean newQuit)
 {
  quit = newQuit;
  if(lastState != null)
   lastState.quit = newQuit;
 }
 
 /**
  * Gets how much time has passed since the last call to {@link update}.
  * @return How much time has passed since the window was last updated, in seconds.
  */
 public double getDeltaTime()
 {
  return lastState.deltaTime;
 }

 /**
  * Gets the system time of the last call to {@link update}.
  * @return The system time of when update was last called, in nanoseconds.
  */
 public double getLastTimeNano()
 {
  return lastState.lastTime;
 }
 
 /**
  * Gets the system time of the last call to {@link update}.
  * @return The system time of when update was last called, in seconds.
  */
 public double getLastTime()
 {
  return lastState.lastTime / 1000000000;
 }
 
 /**
  * Gets whether or not the window is in focus (It is the top-level window that was most recently clicked.)
  * @return True if it is in focus, false otherwise.
  */
 public boolean getFocus()
 {
  return lastState.focus;
 }
 
 /**
  * Gets the name of the window.
  * @return The String name of the window.
  */
 public String getTitle()
 {
  return frame.getTitle();
 }
 
 /**
  * Sets the name of the window.
  * @param name The new name of the window.
  */
 public void setTitle(String name)
 {
  frame.setTitle(name);
 }
 
 /**
  * Sets the size of the window (only works in MODE_STRETCH and MODE_FREE
  * @param width The new width of the window.
  * @param height The new height of the window.
  */
 public void setSize(int width, int height)
 {
  stretchWidth = width;
  stretchHeight = height;
 }
 
 /**
  * FOR INTERNAL USE ONLY - Sets fullscreen to either true or false, making appropriate changes as needed.
  * @param full If the window should be fullscreen or not
  */
 public void setFullscreen(boolean full)
 {
  if(full && !frame.isUndecorated())
  {
   frame.dispose();
   frame.setUndecorated(true);
   frame.pack();
   frame.setVisible(true);
  }
  else if(!full && frame.isUndecorated()){
   frame.dispose();
   frame.setUndecorated(false);
   frame.pack();
   frame.setVisible(true);
  }
 }
 
 /**
  * Plays a sound with the given filename until the sound ends.
  * @param filename The filename of the sound to play.
  */
 public void playSound(String filename)
 {
  File file = new File(filename);
  try {
   AudioInputStream aistream = AudioSystem.getAudioInputStream(file);
   Clip clip = AudioSystem.getClip();
        clip.open(aistream);
        clip.start();
   
        aistream.close();
  } catch (UnsupportedAudioFileException e) {
   System.err.println("Failed to load sound: " + filename);
   e.printStackTrace();
   return;
  } catch (IOException e) {
   System.err.println("Failed to load sound: " + filename);
   e.printStackTrace();
   return;
  } catch (LineUnavailableException e) {
   System.err.println("Failed to load sound: " + filename);
   e.printStackTrace();
   return;
  }
 }
 
 /**
  * Loops the sound file with the given filename
  * @param filename The filename of the sound to loop
  * @return An index identifying this looping sound.  Use stopLoopSound with this index to stop it.
  */
 public int loopSound(String filename)
 {
  File file = new File(filename);
  Clip clip;
  try {
   AudioInputStream aistream = AudioSystem.getAudioInputStream(file);
   clip = AudioSystem.getClip();
   soundClips.add(clip);
        clip.open(aistream);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        aistream.close();
  } catch (UnsupportedAudioFileException e) {
   System.err.println("Failed to load sound: " + filename);
   e.printStackTrace();
   return -1;
  } catch (IOException e) {
   System.err.println("Failed to load sound: " + filename);
   e.printStackTrace();
   return -1;
  } catch (LineUnavailableException e) {
   System.err.println("Failed to load sound: " + filename);
   e.printStackTrace();
   return -1;
  }
  return soundClips.indexOf(clip);
 }
 
 /**
  * Stops a looping sound with the given index.
  * @param index The index of the looping sound, given from the loopSound method.
  */
 public void stopLoopSound(int index)
 {
  soundClips.get(index).stop();
 }
 
 /**
  * Sets the display mode of the EasyWindow
  * MODE_PACK - Window will wrap tightly around the content.  The window size cannot change unless the content changes.
  * MODE_STRETCH - Window can be resized.  The content will be scaled to fit perfectly within it.  Does not maintain the aspect ratio of the content.
  * MODE_SCALE - Window can be resized.  The content will be scaled to fit within it as well as possible while maintaining the aspect ratio.  Excess window size will be cut off.
  * MODE_FREE - Window and content size are independent of each other.
  * MODE_PACK_STRETCH - Window is the size defined by the setSize method.  Content is scaled to fit in the window.  Window size cannot be changed.
  * MODE_FULLSCREEN - Window is made fullscreen.  Black bars surround the content to fix gaps in aspect ratio.
  * MODE_FULLSCREEN_STRETCH - Window is made fullscreen.  Content is stretched to take up all of the screen space.
  * @param mode The new mode to use, as an int constant (one of the MODEs).
  */
 public void setDisplayMode(int mode)
 {
  displayMode = mode;
  if(mode == MODE_FULLSCREEN_STRETCH || mode == MODE_FULLSCREEN)
   setFullscreen(true);
  else
   setFullscreen(false); 
  if(mode == MODE_PACK || mode == MODE_PACK_STRETCH || mode == MODE_FULLSCREEN || mode == MODE_FULLSCREEN_STRETCH)
   frame.setResizable(false);
  else
   frame.setResizable(true);
 }
 
 /**
  * Gets the current display mode.  See setDisplayMode for details on the various modes.
  * @return an int representing the current display mode.
  */
 public int getDisplayMode()
 {
  return displayMode;
 }
 
 /**
  * Starts text capture, a mode where keyboard input is ignored, filling up a String returned by getTextCapture instead of changing the getKey methods.
  * 
  * This method starts the text capture.  Once it has begun, use getTextCapture to get the current contents at any time.
  * The getTextCaptureActive method tells you if the text capture mode is currently consuming keyboard input.
  * The getTextCaptureSent method returns true for only one iteration of the update method, indicating that the send key has just been typed for the text capture.  (Such as pressing enter after a chat message).
  * @param send The key to press to end the text capture and send the text, making getTextCaptureSent return true.  Typing this key will end the text capture.  The contents of the text capture will be preserved until a new one is started.
  * @param cancel The key to press to end the text capture and discard the contents.
  */
 public void startTextCapture(char send, char cancel)
 {
  activeTextSendKey = send;
  activeTextCancelKey = cancel;
  activeTextOn = true;
  activeText = "";
 }
 
 /**
  * Calls the startTextCapture method with common default parameters.
  * The default send key is ENTER.
  * The default cancel key is ESCAPE.
  */
 public void startTextCapture()
 {
  startTextCapture((char)10, (char)27);
 }
 
 /**
  * Gets the content currently collected by the startTextCapture method.
  * @return A String showing all the typed input since text capture began.
  */
 public String getTextCapture()
 {
 return activeText;
 }
 
 /**
  * Returns whether or not a text capture is active.
  * @return Returns true if a text capture is active, or false otherwise.
  */
 public boolean getTextCaptureActive()
 {
  return activeTextOn;
 }
 
 /**
  * Returns whether or not a text capture was just send (the send key for it was pressed).
  * @return Returns true if the text capture was just sent in the prior iteration of the update method.  Any other time, this method returns false.
  */
 public boolean getTextCaptureSent()
 {
  return lastState.activeTextSent;
 }
 
 /**
  * Performs bookkeeping setup for sockets.
  * @param s The socket to add to the EasyWindow's internal lists.
  * @return Returns true if it is successfully added, or false otherwise.
  */
 private boolean buildSocketHelper(Socket s)
 {
  String addressAndPort = (((InetSocketAddress) s.getRemoteSocketAddress()).getAddress()).getHostAddress() + ":" + s.getPort();
  externalSockets.put(addressAndPort,  s);
  ObjectOutputStream oOut;
  ObjectInputStream oIn;
  try {
   oOut = new ObjectOutputStream(s.getOutputStream());
   oIn = new ObjectInputStream(s.getInputStream());
   externalSocketOutputStreams.put(addressAndPort, oOut);
   externalSocketInputStreams.put(addressAndPort, oIn);
   LinkedList<Object> list = new LinkedList<Object>();
   receivedObjects.put(addressAndPort, list);
   Runnable receiveListener = new Runnable() {
    @Override
    public void run() {
     ObjectInputStream oIn = externalSocketInputStreams.get(addressAndPort);
     LinkedList<Object> list = receivedObjects.get(addressAndPort);
     while(externalSockets.containsKey(addressAndPort))
     {
      Object o;
      try {
       o = oIn.readObject();
       list.add(o);
      } catch (ClassNotFoundException e) {
       
       e.printStackTrace();
      } catch (IOException e) {
       
      }
      
     }
    }
     
    };
    Thread receiveThread = new Thread(receiveListener);
    receiveThread.start();
  } catch (IOException e) {
   return false;
  }
  return true;
 }
 
 /**
  * Starts hosting on the specified port, allowing connections over a network, such as the Internet.
  * @param port The port on which to open a connection.
  * @return The address/port used for a LAN connection.  This is what you'll want to port forward in your router.  EX: "192.168.1.1:80".  If it fails, the returned String is an error message instead.
  */
 public String host(int port)
 {
  try {
  
  ServerSocket ss = new ServerSocket(port);
  InetAddress addr = InetAddress.getLocalHost();
  String newHostString = addr.getHostAddress() + ":" + port;
  serverSockets.put(newHostString, ss);
  //This Runnable is what the listener thread will be doing.
  Runnable hostListen = new Runnable() {

   @Override
   public void run() {
    while(serverSockets.containsKey(newHostString))
    {
     try {
      Socket newSocket = serverSockets.get(newHostString).accept();
      buildSocketHelper(newSocket);
     } catch (IOException e) {
      System.err.println("Failed to make connection as host");
     }
    }
   }
    
   };
   Thread hostThread = new Thread(hostListen);
   hostThread.start();
  return newHostString;
 } catch (UnknownHostException e) {
  return "FAILED TO HOST - UNKKNOWNHOSTEXCEPTION";
 } catch (IOException e) {
  return "FAILED TO HOST - IOEXCEPTION";
 }
  
 }
 
 /**
  * Returns a list of all connections to other computers managed by this window.
  * @return An array of address/port Strings.  EX: ["192.168.1.1:80", "192.168.1.1:20"]
  */
 public String[] getConnections()
 {
  return externalSocketOutputStreams.keySet().toArray(new String[0]);
 }
 
 /**
  * Returns a list of all serverSockets of this window.  That is, all of the ports this window is listening on.
  * @return An array of address/port Strings.  EX: ["192.168.1.1:80", "192.168.1.1:20"]
  */
 public String[] getHosts()
 {
  return serverSockets.keySet().toArray(new String[0]);
 }
 
 /**
  * Connects to the specified address/port, enabling communication over a network.
  * @param addressAndPort The address and port from which to receive.  EX: "192.168.1.1:80"
  * @return Returns true if it successfully connection, or false if it fails.
  */
 public boolean connect(String addressAndPort)
 {
  int index = addressAndPort.lastIndexOf(':');
  String address = addressAndPort.substring(0, index);
  int port = Integer.parseInt(addressAndPort.substring(index+1, addressAndPort.length()));
  try {
  Socket s = new Socket(InetAddress.getByName(address), port);
  buildSocketHelper(s);
 } catch (UnknownHostException e) {
  return false;
 } catch (IOException e) {
  return false;
 }
  return true;
 }
 
 /**
  * Sends a Serializable Object accross the specified address/port.
  * If a connection has not already been made on the specified address/port, this method will attempt to do so.
  * @param addressAndPort The address and port to which the method will send an Object.  EX: "192.168.1.1:80"
  * @param content The Serializable Object to send.  Add "implements Serializable" to the class to make it send-able.
  * @return Returns the object sent, or null if it failed to send.
  */
 public Serializable send(String addressAndPort, Serializable content)
 {
  if(!externalSockets.containsKey(addressAndPort))
   connect(addressAndPort);
  try {
  ObjectOutputStream oOut = externalSocketOutputStreams.get(addressAndPort);
  oOut.writeObject(content);
  return content;
 } catch (IOException e) {
  return null;
 }
 }
 
 /**
  * Returns whether or not there is an Object waiting to be received from the specified address/port.
  * @param addressAndPort The address and port from which to receive.  EX: "192.168.1.1:80"
  * @return Returns true if there is an Object waiting to be received, or false otherwise.
  */
 public boolean hasReceive(String addressAndPort)
 {
  LinkedList<Object> list = receivedObjects.get(addressAndPort);
  return list.size() > 0;
 }
 
 /**
  * Gets the most recently received Object from the specified address/port connection.
  * Throws a NullPointerException if the specified addressAndPort is not an established connection.
  * @param addressAndPort The address and port from which to receive.  EX: "192.168.1.1:80"
  * @return A Serializable Object, sent over a socket to be received.
  */
 public Object receive(String addressAndPort)
 {
 LinkedList<Object> list = receivedObjects.get(addressAndPort);
 if(list.size() > 0)
  return list.remove();
 else
  return null;
 }
 
 /**
  * FOR INTERNAL USE ONLY - Sets the image to be displayed by the window.  Normally only for internal use, but rare cases may exist where you want to call it directly.
  * @param bi The BufferedImage to display.
  */
 public void setImage(BufferedImage bi)
 {
  contents = bi;
  BufferedImage bi2 = bi;
  if(displayMode == MODE_PACK_STRETCH )
  {
   imageWidth = stretchWidth;
   imageHeight = stretchHeight;
  }
  else if(displayMode == MODE_STRETCH)
  {
   imageWidth = frame.getContentPane().getComponent(0).getWidth();
   imageHeight = frame.getContentPane().getComponent(0).getHeight();
  }
  else if(displayMode == MODE_SCALE)
  {
   int frameWidth = frame.getContentPane().getComponent(0).getWidth();
   int frameHeight = frame.getContentPane().getComponent(0).getHeight();
   imageWidth = frameWidth;
   imageHeight = frameHeight;
   double widthScale = (1.0 * imageWidth )/ bi.getWidth();
   double heightScale = (1.0 * imageHeight )/ bi.getHeight();
   if(widthScale * bi.getHeight() > imageHeight)
    imageWidth = (int) (bi.getWidth() * heightScale);
   else
    imageHeight = (int) (bi.getHeight() * widthScale);
   BufferedImage bTemp = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
   Graphics gTemp = bTemp.getGraphics();
   gTemp.drawImage(bi, 0, 0, bTemp.getWidth(), bTemp.getHeight(), 0, 0, bi.getWidth(), bi.getHeight(), null);
   gTemp.dispose();
   bi = bTemp;
   imageWidth = frameWidth;
   imageHeight = frameHeight;
   
  }
  else if(displayMode == MODE_FULLSCREEN || displayMode == MODE_FULLSCREEN_STRETCH)
  {
   GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
   imageWidth = gd.getDisplayMode().getWidth();
   imageHeight = gd.getDisplayMode().getHeight();
  }
  if(displayMode == MODE_STRETCH || displayMode == MODE_PACK_STRETCH || displayMode == MODE_FULLSCREEN_STRETCH)
  {
   bi2 = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
   Graphics g2 = bi2.getGraphics();
   g2.drawImage(bi, 0, 0, bi2.getWidth(), bi2.getHeight(), 0, 0, bi.getWidth(), bi.getHeight(), null);
   g2.dispose();
  }
  if(displayMode == MODE_FULLSCREEN || displayMode == MODE_SCALE)
  {
   bi2 = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
   Graphics g2 = bi2.getGraphics();
   xBorder = Math.max(0,  (imageWidth - bi.getWidth()) / 2);
   yBorder = Math.max(0,  (imageHeight - bi.getHeight()) / 2);
   double widthScale = (1.0 * imageWidth )/ bi.getWidth();
   double heightScale = (1.0 * imageHeight )/ bi.getHeight();
   if(widthScale * bi.getHeight() > imageHeight)
   {
    yBorder = 0;
    xBorder = (int) (xBorder / heightScale);
   }
   else
   {
    xBorder = 0;
    yBorder = (int) (yBorder / widthScale);
   }
   g2.setColor(Color.BLACK);
   g2.fillRect(0, 0, xBorder, imageHeight);
   g2.fillRect(imageWidth - xBorder, 0, xBorder, imageHeight);
   g2.fillRect(0, 0, imageWidth, yBorder);
   g2.fillRect(0, imageHeight - yBorder, imageWidth, yBorder);
   g2.drawImage(bi, xBorder, yBorder, bi2.getWidth() - xBorder, bi2.getHeight() - yBorder, 0, 0, bi.getWidth(), bi.getHeight(), null);
   g2.dispose();
  }
  
  
  imageWidth = bi2.getWidth();
  imageHeight = bi2.getHeight();
  BufferedImage newImage = new BufferedImage(bi2.getWidth(), bi2.getHeight(), bi2.getType());
  Graphics g = newImage.getGraphics();
  g.drawImage(bi2, 0, 0, null);
  g.dispose();
  ((JLabel)frame.getContentPane().getComponent(0)).setIcon(new ImageIcon(bi2));
  if(displayMode == MODE_PACK || displayMode == MODE_PACK_STRETCH || displayMode == MODE_FULLSCREEN || displayMode == MODE_FULLSCREEN_STRETCH || firstUpdate)
   frame.pack();
  
 }

 /**
  * FOR INTERNAL USE ONLY - Sets the image to be displayed by the window.  Normally only for internal use, but rare cases may exist where you want to call it directly.
  * @param image The int[][][] to display.
  */
 public void setImage(int[][][] image)
 {
 setImage(getImageOfArray(image));
 }
 
 /**
  * FOR INTERNAL USE ONLY - Removes transparency from a BufferedImage (returning a copy of it).
  * @param bi The BufferedImage from which to remove transparency.
  * @param backgroundColor The Color to fill in the transparent sections.
  * @return A copy of the given image, with all transparency replaced with backgroundColor.
  */
 public static BufferedImage removeTransparency(BufferedImage bi, Color backgroundColor) {
  BufferedImage newImage = new BufferedImage(bi.getWidth(), bi.getHeight(), bi.getType());
  Graphics g = newImage.getGraphics();
  g.drawImage(bi, 0, 0, backgroundColor, null);
  g.dispose();
  return newImage;
 }
 
 /**
  * FOR INTERNAL USE ONLY - Converts an int[][][] into a BufferedImage
  * @param image int[][][] to convert into a BufferedImage
  */
 private static BufferedImage getImageOfArray(int[][][] image)
 {
  if(image.length == 0 || image[0].length == 0 || image[0][0].length < 3)
   return null;
 BufferedImage bi = new BufferedImage(image.length, image[0].length, BufferedImage.TYPE_INT_ARGB);
 for(int x = 0; x < image.length; x++)
  for(int y = 0; y < image[0].length; y++)
  {
   Color c = new Color(image[x][y][0], image[x][y][1], image[x][y][2]);
   bi.setRGB(x,  y, c.getRGB());
  }
 return bi;
 }
 
 /**
  * FOR INTERNAL USE ONLY - Gets an int[][][] from a BufferedImage.
  * @param bi The BufferedImage to convert into an int[][][].
  * @return An int[][][] made from a BufferedImage.  Transparency is replaced with white.
  */
 private static int[][][] getArrayOfImage(BufferedImage bi) {
  bi = removeTransparency(bi, Color.WHITE);
  int array[][][] = new int[bi.getWidth()][bi.getHeight()][3];
  for (int x = 0; x < array.length; x++)
   for (int y = 0; y < array[x].length; y++) {
    Color c = new Color(bi.getRGB(x, y));
    array[x][y][0] = c.getRed();
    array[x][y][1] = c.getGreen();
    array[x][y][2] = c.getBlue();
   }
  return array;
 }
 
 /**
  * FOR INTERNAL USE ONLY
  */
 @Override
 public void mouseClicked(MouseEvent me) {
  // TODO Auto-generated method stub
  
 }

 /**
  * FOR INTERNAL USE ONLY
  */
 @Override
 public void mouseEntered(MouseEvent me) {
  // TODO Auto-generated method stub
  
 }

 /**
  * FOR INTERNAL USE ONLY
  */
 @Override
 public void mouseExited(MouseEvent me) {
  // TODO Auto-generated method stub
  
 }

 /**
  * FOR INTERNAL USE ONLY
  */
 @Override
 public void mousePressed(MouseEvent me) {
  if(me.getButton() <= MOUSE_BUTTON_COUNT)
   mouseButtonsLive[me.getButton() - 1] = true;
 }


 @Override
 public void mouseReleased(MouseEvent me) {
  if(me.getButton() <= MOUSE_BUTTON_COUNT)
   mouseButtonsLive[me.getButton() - 1] = false;
 }

 /**
  * FOR INTERNAL USE ONLY
  */
 @Override
 public void mouseDragged(MouseEvent me) {
  mouseX = me.getX();
  mouseY = me.getY();
 }

 /**
  * FOR INTERNAL USE ONLY
  */
 @Override
 public void mouseMoved(MouseEvent me) {
  mouseX = me.getX();
  mouseY = me.getY();
 }
 
 /**
  * FOR INTERNAL USE ONLY
  */
 @Override
 public void keyPressed(KeyEvent ke) {
  if(ke.getKeyCode() < KEYBOARD_KEY_COUNT && !activeTextOn)
  {
   if(!QUEUED_INPUT)
    keysLive[ke.getKeyCode()] = true;
   else
    keyUpdates[ke.getKeyCode()].add(true);
  }
  
  else if(ke.getKeyChar() == 8)//Backspace
  {
   //activeText = activeText.substring(0, activeText.length() - 1);
  }
 
 }

 /**
  * FOR INTERNAL USE ONLY
  */
 @Override
 public void keyReleased(KeyEvent ke) {
  if(ke.getKeyCode() < KEYBOARD_KEY_COUNT)
  {
   if(!QUEUED_INPUT)
    keysLive[ke.getKeyCode()] = false;
   else
    keyUpdates[ke.getKeyCode()].add(false);
  }
 }

 /**
  * FOR INTERNAL USE ONLY
  */
 @Override
 public void keyTyped(KeyEvent ke) {
  if(activeTextOn) { 
   if(ke.getKeyChar() != 8 && ke.getKeyChar() != activeTextSendKey && ke.getKeyChar() != activeTextCancelKey)
   {
   activeText += ke.getKeyChar();
   }
   if(ke.getKeyChar() == 8)
   {
   activeText = activeText.substring(0, activeText.length() - 1);
   }
   if(ke.getKeyChar() == activeTextCancelKey)
   {
   activeTextOn = false;
   activeText = "";
   }
   if(ke.getKeyChar() == activeTextSendKey)
   {
   activeTextOn = false;
   activeTextSent = true;
   }
  }
 }
 
 /**
  * FOR INTERNAL USE ONLY
  */
 @Override
 public void mouseWheelMoved(MouseWheelEvent mwe) {
  mouseWheelDelta = mwe.getWheelRotation();
 }
 
 /**
  * FOR INTERNAL USE ONLY
  */
 @Override
 public void windowGainedFocus(WindowEvent we) {
  focus = true;
 }
 
 /**
  * FOR INTERNAL USE ONLY
  */
 @Override
 public void windowLostFocus(WindowEvent we) {
  focus = false;
 }

 /**
  * FOR INTERNAL USE ONLY
  */
@Override
public void windowActivated(WindowEvent arg0) {
 // TODO Auto-generated method stub
 
}

/**
  * FOR INTERNAL USE ONLY
 */
@Override
public void windowClosed(WindowEvent arg0) {
 // TODO Auto-generated method stub
 
}

/**
  * FOR INTERNAL USE ONLY
 */
@Override
public void windowClosing(WindowEvent arg0) {
 quit = true;
 
}

/**
  * FOR INTERNAL USE ONLY
 */
@Override
public void windowDeactivated(WindowEvent arg0) {
 // TODO Auto-generated method stub
 
}

/**
  * FOR INTERNAL USE ONLY
 */
@Override
public void windowDeiconified(WindowEvent arg0) {
 // TODO Auto-generated method stub
 
}

/**
  * FOR INTERNAL USE ONLY
 */
@Override
public void windowIconified(WindowEvent arg0) {
 // TODO Auto-generated method stub
 
}

/**
  * FOR INTERNAL USE ONLY
 */
@Override
public void windowOpened(WindowEvent arg0) {
 // TODO Auto-generated method stub
 
}

/**
 * FOR INTERNAL USE ONLY
*/
@Override
public void componentHidden(ComponentEvent e) {
 // TODO Auto-generated method stub
 
}

/**
 * FOR INTERNAL USE ONLY
*/
@Override
public void componentMoved(ComponentEvent e) {
 // TODO Auto-generated method stub
 
}

/**
 * FOR INTERNAL USE ONLY
*/
@Override
public void componentResized(ComponentEvent e) {
 // TODO Auto-generated method stub
 
}

/**
 * FOR INTERNAL USE ONLY
*/
@Override
public void componentShown(ComponentEvent e) {
 // TODO Auto-generated method stub
 
}
 
}
