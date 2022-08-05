import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Wormhole extends Entity {
	
	//The image of a wormhole, loaded as a statically
	private static BufferedImage wormholeImage; 
	
	//Loads the wormhole image.  Will only be called once by Lab4
	public static void loadImages() {
		wormholeImage = null;
		try {
			wormholeImage = ImageIO.read(new File ("Wormhole.png"));
		} catch (IOException e) {
			
		}
	}
	
	//Constructor
	public Wormhole(double xCenter, double yCenter, double width, double height) {
		super(xCenter, yCenter, width, height);
		
	}
	
	//Updates the position of the wormhole
	public void update(EasyWindow ew) {
		setXCenter(ew.getMouseX());
		setYCenter(ew.getMouseY());
		
	}
	
	//Draws the wormhole using the g.drawImage method
	@Override
	public void draw(Graphics g) {
		g.drawImage(wormholeImage, (int) getHitbox().x, (int) getHitbox().y, (int) getHitbox().width,
				(int) getHitbox().height, null);
		
	}


}
