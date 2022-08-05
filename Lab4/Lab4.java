import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Color;
import java.util.ArrayList;
public class Lab4
{
  public static final int SCREEN_WIDTH = 500;
  public static final int SCREEN_HEIGHT = 500;
  public static final int ASTEROID_SIZE = 50;
  public static final int WORMHOLE_SIZE = 100;
  public static final int ASTEROID_Y_POSITION = -40;
  public static final int LEFT_CLICK = 1;
  
  
  public static void addAsteroid(ArrayList<Entity> entities, Entity wormhole)
  {
    entities.add(new Asteroid(Math.random() * SCREEN_WIDTH, ASTEROID_Y_POSITION, ASTEROID_SIZE, ASTEROID_SIZE, wormhole));
  }
  
  public static void main(String[] args)
  {
 EasyWindow ew = new EasyWindow();
 BufferedImage background = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_ARGB);
 BufferedImage screenImage = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_ARGB);
 BufferedImage canvas = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_ARGB);
 ArrayList<Entity> entities = new ArrayList<Entity>();
 Graphics screen = screenImage.getGraphics();
 Wormhole wh = new Wormhole(0, 0, WORMHOLE_SIZE, WORMHOLE_SIZE);
 entities.add(wh);
 double asteroidSpawnDelay = 0;
 boolean drawHitboxes = true;
 
 background = EasyWindow.loadImage("void.png");
 Wormhole.loadImages();
 Asteroid.loadImages();
 
 while(!ew.getQuit())
{
   if(ew.getMouseButtonFirst(LEFT_CLICK))
        drawHitboxes = !(drawHitboxes);
   
   asteroidSpawnDelay -= ew.getDeltaTime();
   if(asteroidSpawnDelay <= 0)
   {
    asteroidSpawnDelay = Math.random() * 2 + 1;
    addAsteroid(entities, wh);
   }
   Graphics g = canvas.getGraphics();
   g.drawImage(background, 0, 0, canvas.getWidth(), canvas.getHeight(), null);
   for(Entity e : entities)
   {
    e.update(ew);
   }
   if(drawHitboxes)
     for(Entity e : entities)
     {
       e.drawHitbox(g);
     }
   for(Entity e : entities)
   {
    e.draw(g);
   }
   for(int i = entities.size() - 1; i >= 0; i--)
   {
    if(entities.get(i).getKill())
      entities.remove(i);
   }
 screen.drawImage(canvas, 0, 0, null);
 ew.update(screenImage);
 g.dispose();
}
  }
}