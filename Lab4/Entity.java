import java.awt.Graphics;
import java.awt.Rectangle;

public abstract class Entity {
	
	private double xCenter;   //the x center of the entity
	private double yCenter;   //the y center of the entity
	private double width;     //width of the entity
	private double height;    //height of the entity
	private boolean kill;;    //when true, the entity will be remove
	
	public Entity(double xCenter, double yCenter, double width, double height) {
		this.xCenter = xCenter;
		this.yCenter = yCenter;
		this.width = width;
		this.height = width;
		kill = false;
		
	}
	
	//Updates the position and other attributes
	public abstract void update(EasyWindow ew);
	
	//draw the entity
	public abstract void draw(Graphics g);
	
	//converts the center, width, and height into a Rectangle class
	public Rectangle getHitbox() {
		
		Rectangle r = new Rectangle((int) (xCenter - width / 2), (int) (yCenter - height / 2), (int) width, (int) height );
		 
		return r;
		
		
	}
	
	//Draws the Rectangle from getHitbox() onto the screen
	//Using g.drawRect(x, y, width, height); is about all you need.
	public void drawHitbox(Graphics g) {
		getHitbox();
		g.drawRect((int) getHitbox().getX(), (int) getHitbox().getY(), (int) getHitbox().width, (int) getHitbox().height);
		
	}
	
	
	//Simple set method
	public void setXCenter(double x) {
		xCenter = x;
	}
	
	//Simple get method
	public double getXCenter() {
		return xCenter;
	}
	
	//Simple set method
	public void setYCenter(double y) {
		yCenter = y;
		
	}
	
	//Simple get method
	public double getYCenter() {
		return yCenter;
	}
	
	//Simple set method
	public void setKill(boolean kill) {
		this.kill = kill;
	}
	
	//Simple get method
	public boolean getKill() {
		return kill;
		
	}

	

}
