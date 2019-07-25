package mypackage;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;

/*
 * COLOR WORKS
 * Start with a big picture of 1000*1000
 * small pictures of 50*5
 * "ref" is the initial image
 * "Thumbnails" or "thumbs" are the small pictures
 * "Mozaic" is the final
 * 
 * !Make it only take average of top percentage of colours!
 */
public class ImageMozaicGenerator {
	//private static String srcFolder;
	private static File refSrc;
	private static File thumbSrcDir;
	//how much of the thumbnail is useed????
	//should be a percentage?
	//noise tolerance?
	private float marginthingy = 0.7f;
	
	//reference image to be converted
	private BufferedImage ref;
	//broken up tiles from refImage, arranged l2r, t2b
	private Thumbnail[] refThumbs;
	//images to be used in the final mozaic
	private Thumbnail[] thumbnails;
	
	public ImageMozaicGenerator(){
		try {
			ref = ImageIO.read(refSrc);
		}catch(IOException e) {
			e.printStackTrace();
		}
		getRefTiles();
		thumbnails = getThumbnails(thumbSrcDir);
		substituteThumbs();
		try {
			saveImage();
		}catch(IOException e) {
			e.printStackTrace();
		}
		
	}
	private void saveImage() throws IOException{
		BufferedImage finalImg = new BufferedImage(1000,1000,BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = finalImg.createGraphics();
		File output;
		int n = 0;
		do {
			output = new File(refSrc.getParentFile(), "MozaicOutput"+((n<10)?"0"+n:n)+".jpg");
			n++;
		}while (output.exists());
		output.createNewFile();
		//System.out.println(output.exists());
		int i=0;
		for(int y=0;y<1000;y+=50) {
			for(int x=0;x<1000;x+=50) {
				g2d.drawImage(refThumbs[i].img, null, x, y);
				i++;
			}
		}
		ImageIO.write(finalImg, "jpg", output);
	}
	private void substituteThumbs() {
		for(int i=0;i<refThumbs.length;i++) {
			double distance = Math.abs(thumbnails[0].avgColor.distanceFrom(refThumbs[i].avgColor));
			int idx = 0;
			for(int j=1;j<thumbnails.length;j++) {
				double d = Math.abs(thumbnails[j].avgColor.distanceFrom(refThumbs[i].avgColor));
				if(d < distance) {
					distance = d;
					idx = j;
				}
			}
			refThumbs[i] = thumbnails[idx];
		}
	}
	private void getRefTiles() {
		//need to make evenly distribute 50*50px tiles over any sized image
		refThumbs = new Thumbnail[(1000/50)*(1000/50)];
		int i=0;
		for(int y=0;y<1000;y+=50) {
			for(int x=0;x<1000;x+=50) {
				refThumbs[i] = new Thumbnail(ref.getSubimage(x, y, 50, 50));
				//System.out.println(refImageComp[i].brightness);
				i++;
			}
		}
	}
	private Thumbnail[] getThumbnails(File directory) {
		//Need to compress incomng images to 50*50px
		File[] images = directory.listFiles(new FileFilter() {
			private final String[] acceptedExtensions = new String[] {".jpg", ".jpeg", ".png", ".gif"};
			public boolean accept(File file) {
				for(String ext : acceptedExtensions) {
					if(file.getName().toLowerCase().endsWith(ext)) {
						return true;
					}
				}
				return false;
			}
		});
		Thumbnail[] thumbs = new Thumbnail[images.length];
		for(int i=0;i<images.length;i++) {
			try {
				thumbs[i] = new Thumbnail(images[i]);
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
		return thumbs;
	}
	public static void main(String[] args) {
		refSrc = new File(args[0]);
		thumbSrcDir = new File(args[1]);
		new ImageMozaicGenerator();
	}
	class Thumbnail{
		BufferedImage img;
		String src;
		
		int width;
		int height;
		
		Color avgColor;
		public Thumbnail(File src) throws IOException{
			this.src = src.getAbsolutePath();
			img = Scalr.resize(ImageIO.read(src), 50);
			width = img.getWidth();
			height = img.getHeight();
			avgColor = getColorAvg();
			//brightness = getBrightness();
			//for each pixel in img, get Color (new Color(img.getRGB(x,y));)
			//red = color.getRed, etc
			//redAvg = avg(red from all pixels)
			
		}
		public Thumbnail(BufferedImage img) {
			this.img = img;
			width = img.getWidth();
			height = img.getHeight();
			avgColor = getColorAvg();
			//brightness = getBrightness();
		}
		private Color getColorAvg() {
			int redAvg = 0;
			int greenAvg = 0;
			int blueAvg = 0;
			
			List<Integer> redValues = new ArrayList<Integer>();
			List<Integer> greenValues = new ArrayList<Integer>();
			List<Integer> blueValues = new ArrayList<Integer>();
			
			for(int h=0;h<height;h++) {
				for(int w=0;w<width;w++) {
					Color c = new Color(img.getRGB(w,h));
					redValues.add(c.getRed());
					greenValues.add(c.getGreen());
					blueValues.add(c.getBlue());
					
					//redAvg += c.getRed();
					//greenAvg += c.getGreen();
					//blueAvg += c.getBlue();
				}
			}
			redValues = getTopPercentage(redValues);
			greenValues = getTopPercentage(greenValues);
			blueValues = getTopPercentage(blueValues);
			for(Integer i:redValues) {
				redAvg += i;
			}
			for(Integer i:greenValues) {
				greenAvg += i;
			}
			for(Integer i:blueValues) {
				blueAvg += i;
			}
			redAvg /= redValues.size();
			greenAvg /= greenValues.size();
			blueAvg /= blueValues.size();
			//System.out.println(redAvg+", "+greenAvg+", "+blueAvg);
			//redAvg /= height*width;
			//greenAvg /= height*width;
			//blueAvg /= height*width;
			//System.out.println(redAvg+", "+greenAvg+", "+blueAvg);
			
			
			return new Color(redAvg, greenAvg, blueAvg);
		}
		private List<Integer> getTopPercentage(List<Integer> i) {
			i.sort(null);
			Collections.reverse(i);
			int numfrompercent = Math.round(i.size() * marginthingy);
			i = i.subList(0, numfrompercent);
			return i;
		}
	}
	class Color extends java.awt.Color{
		public Color(int arg) {
			super(arg);
		}
		public Color(int r, int g, int b) {
			super(r,g,b);
		}
		public double distanceFrom(Color c) {
			int redDiff = this.getRed()-c.getRed();
			int greenDiff = this.getGreen()-c.getGreen();
			int blueDiff = this.getBlue()-c.getBlue();
			return Math.sqrt(Math.pow(redDiff, 2)+Math.pow(greenDiff, 2)+Math.pow(blueDiff, 2));
		}
	}
}