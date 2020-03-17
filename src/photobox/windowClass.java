package photobox;


import processing.core.PApplet;
import processing.core.PFont;


public class windowClass extends PApplet {
	
	PFont font;
	
	public void settings() {
		  size(1600, 900);
		  fullScreen();
		}
	
	public void setup() {
		background(51);
		font = createFont("Arial Bold",48);
	}
	
	public void draw() {
		background(51);
		if(launcher.curPic != null) {
			image(launcher.curPic, 125, 0);
		}
		textFont(font,36);
		text(frameRate,20,20);
	}
	
	
	
	
}
