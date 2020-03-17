package photobox;


import java.awt.Graphics2D;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

import processing.core.PConstants;
import processing.core.PImage;
import x.mvmn.gphoto2.jna.Camera;
import x.mvmn.gphoto2.jna.Camera.ByReference;
import x.mvmn.gphoto2.jna.Gphoto2Library;

public class launcher {

	public static PImage curPic;
	
	public static void main(String[] args) throws Exception {
		
		windowClass.main("photobox.windowClass");
		
		
		final Camera camera = newCamera();

		final PointerByReference context = newContext();
		initCamera(camera, context);
		final AtomicBoolean finish = new AtomicBoolean(false);
		Thread previewThread = new Thread() {
			public void run() {
				while (!finish.get()) {
					final PointerByReference pbrFile = capturePreview(camera, context);
					try {
						final BufferedImage image = ImageIO.read(new ByteArrayInputStream(getCameraFileData(pbrFile, camera, context)));
						
						bufferToPImage(image);
						Thread.yield();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						freeCameraFile(pbrFile);
					}
				}

				exitCam(camera, context);
			}
		};
		previewThread.start();
	}

	public static int check(int retVal) {
		if (retVal < 0) {
			System.err.println("Error " + retVal);
			new Exception().printStackTrace();
		}

		return retVal;
	}

	public static int exitCam(Camera camera, PointerByReference context) {
		return check(Gphoto2Library.INSTANCE.gp_camera_exit(camera, context));
	}

	public static int freeCameraFile(PointerByReference pbrFile) {
		return check(Gphoto2Library.INSTANCE.gp_file_unref(pbrFile));
	}

	public static int initCamera(Camera camera, PointerByReference context) {
		return check(Gphoto2Library.INSTANCE.gp_camera_init(camera, context));
	}

	public static PointerByReference newContext() {
		return Gphoto2Library.INSTANCE.gp_context_new();
	}

	public static ByReference newCamera() {
		Camera.ByReference[] p2CamByRef = new Camera.ByReference[] { new Camera.ByReference() };
		check(Gphoto2Library.INSTANCE.gp_camera_new(p2CamByRef));
		return p2CamByRef[0];
	}

	public static PointerByReference capturePreview(Camera camera, PointerByReference context) {
		PointerByReference pbrFile = new PointerByReference();
		{
			check(Gphoto2Library.INSTANCE.gp_file_new(pbrFile));
			PointerByReference pFile = new PointerByReference();
			pFile.setPointer(pbrFile.getValue());
			pbrFile = pFile;
		}
		check(Gphoto2Library.INSTANCE.gp_camera_capture_preview(camera, pbrFile, context));
		return pbrFile;
	}

	public static byte[] getCameraFileData(PointerByReference cameraFile, Camera camera, PointerByReference context) {
		PointerByReference pref = new PointerByReference();
		LongByReference longByRef = new LongByReference();
		int captureRes = check(Gphoto2Library.INSTANCE.gp_file_get_data_and_size(cameraFile, pref, longByRef));
		if (captureRes >= 0) {
			return pref.getValue().getByteArray(0, (int) longByRef.getValue());
		} else {
			return null;
		}
	}
	
	public static BufferedImage scaleImage(BufferedImage in) {
		int w = in.getWidth();
		int h = in.getHeight();
		BufferedImage after = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		AffineTransform at = new AffineTransform();
		at.scale(2.0, 2.0);
		AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
		after = scaleOp.filter(in, after);
		return after;
	}
	
	private static BufferedImage createFlipped(BufferedImage image)
    {
        AffineTransform at = new AffineTransform();
        at.concatenate(AffineTransform.getScaleInstance(-1, 1));
        at.concatenate(AffineTransform.getTranslateInstance(-image.getWidth(), 0));
        return createTransformed(image, at);
    }
	
	private static BufferedImage createTransformed(
	        BufferedImage image, AffineTransform at)
	    {
	        BufferedImage newImage = new BufferedImage(
	            image.getWidth(), image.getHeight(),
	            BufferedImage.TYPE_INT_ARGB);
	        Graphics2D g = newImage.createGraphics();
	        g.transform(at);
	        g.drawImage(image, 0, 0, null);
	        g.dispose();
	        return newImage;
	    }
	
	public static void bufferToPImage(BufferedImage in) {
		in = createFlipped(in);
		PImage img=new PImage(in.getWidth(),in.getHeight(),PConstants.ARGB);
        in.getRGB(0, 0, img.width, img.height, img.pixels, 0, img.width);
        img.updatePixels();
        img.resize(1350, 900);
        curPic = img;
	}
	
}
