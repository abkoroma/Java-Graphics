import java.awt.Graphics;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;


public class Asteroid extends Entity {
	
	public static BufferedImage asteroidImage;  //The image of the asteroid to be loaded statically
	double xVel;        //The x velocity of the asteroid
	double yVel;        //The y velocity of the asteroid
	double speed; //The speed of the asteroid.  I recommend setting it randomly.  It should be high, around 100.
	Entity target;        //The target of the asteroid (will be the wormhole)
	
	
	//Loads the asteroid image.  WIll only be called once by Lab4
	public static void loadImages() {
		asteroidImage = null;
		try {
			asteroidImage = ImageIO.read(new File ("asteroid.png"));
		} catch (IOException e) {
			
		}
		
	}
	
	//Constructor
	public Asteroid(double xCenter, double yCenter, double width, double height, Entity target) {
		super(xCenter, yCenter, width, height);
		this.target = target ;
		speed = Math.random() * 50 + 50;
	}
	
	
	public Point2D.Double getDirection() {
	  double dx = target.getXCenter() - getXCenter();
	  double dy = target.getYCenter() - getYCenter();
	  double distance = Math.max(Math.sqrt(dx * dx + dy * dy), 0.000001);
	  return new Point2D.Double(dx / distance, dy / distance);
	}
	
	//Will tell if asteroid intersects the center of Entity e. 
	//You'll want to make a new Point class out of e.getXCenter() and e.getYCenter() for it.  
	//Rectangle has a method called contains to do this for you, if you call getHitbox() first
	public boolean collision(Entity e) {
		return getHitbox().contains(e.getXCenter(), e.getYCenter());
		
	}
	
	//Will adjust xPos and yPos by xVel * time and yVel * time.  
	//Will set xVel and yVel as described in the getDirection method.  
	//Will set kill to true if the collision method returns true, 
	//where the Entity e parameter is the asteroid's target.
	public void update(EasyWindow ew) {
		
		xVel = getDirection().getX() * speed;
		yVel =  getDirection().getY() * speed;
		
		setXCenter(getXCenter() +  xVel * ew.getDeltaTime());
		setYCenter(getYCenter() +  yVel * ew.getDeltaTime());
		
		
		//double xPos = xVel * ew.getDeltaTime();
		//double yPos = yVel * ew.getDeltaTime();
		
		if(collision(target)) {
			setKill(true);
		}
		
		
		
	}
	
	//draws the asteroid image
	@Override
	public void draw(Graphics g) {
		g.drawImage(asteroidImage, (int) getHitbox().x, (int) getHitbox().y, (int) getHitbox().width,
				(int) getHitbox().height, null);
		
	}
	
	


}
