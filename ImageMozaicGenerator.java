package mypackage;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
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
	//how much of the thumbnail is useed????
	//should be a percentage?
	//noise tolerance?
	private static float marginthingy = 1.0f;
	
	private static boolean blackandwhite = false;
	private static String saveDestination = "";
	
	private static int xNumOfTiles = 100;
	private static int yNumOfTiles = 100;
	
	//reference image to be converted
	private BufferedImage refImg;
	//broken up tiles from refImage, arranged l2r, t2b
	private Thumbnail[] refTiles;
	//images to be used in the final mozaic
	protected List<Thumbnail> thumbnails;
	
	protected BufferedImage mozaic;
	
	public ImageMozaicGenerator(){
		thumbnails = new ArrayList<Thumbnail>();
	}
	public void setReferenceImage(String src) throws IOException{
		refSrc = new File(src);
		refImg = ImageIO.read(refSrc);
	}
	public BufferedImage getReferenceImage() {
		return refImg;
	}
	private void addThumbnails(String src) throws IOException {
		File file = new File(src);
		if(file.isDirectory()) {
			addThumbnailsFromDir(file);
		}else {
			if(file.isFile()) {
				addThumbnailsFromFile(file);
			}
		}
	}
	protected void addThumbnailsFromFile(File file) throws IOException {
		thumbnails.add(new Thumbnail(file));
	}
	protected void addThumbnailsFromDir(File dir) throws IOException {
		File[] files = dir.listFiles(new FileFilter() {
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
		for(File file:files) {
			thumbnails.add(new Thumbnail(file));
		}
	}
	protected void generateMozaic() {
		int tileWidth = refImg.getWidth()/xNumOfTiles;
		int tileHeight = refImg.getHeight()/yNumOfTiles;
		refTiles = getRefTiles(tileWidth, tileHeight);
		scaleThumbnails(tileWidth, tileHeight);
		substituteThumbnails();
		
		mozaic = new BufferedImage(refImg.getWidth(),refImg.getHeight(),BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = mozaic.createGraphics();
		int i=0;
		for(int y=0;y<mozaic.getHeight();y+=tileHeight) {
			for(int x=0;x<mozaic.getWidth();x+=tileWidth) {
				g2d.drawImage(refTiles[i].img, null, x, y);
				i++;
			}
		}
	}
	private Thumbnail[] getRefTiles(int width, int height) {
		List<Thumbnail> tiles = new ArrayList<Thumbnail>();
		for(int y=0;y<yNumOfTiles;y++) {
			for(int x=0;x<xNumOfTiles;x++) {
				try {
					tiles.add(new Thumbnail(refImg.getSubimage(x*width, y*height, width, height)));
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		Thumbnail[] tileArray = new Thumbnail[tiles.size()];
		System.out.println(tileArray.length);
		return tiles.toArray(tileArray);
	}
	private void scaleThumbnails(int width, int height) {
		for(Thumbnail thumbnail:thumbnails) {
			thumbnail.scaleImage(width, height);
		}
	}
	private void substituteThumbnails() {
		for(Thumbnail thumbnail:thumbnails) {
			thumbnail.getAverageColor(marginthingy);
		}
		for(int i=0;i<refTiles.length;i++) {
			double distance = Math.abs(thumbnails.get(0).avgColor.distanceFrom(refTiles[i].avgColor));
			int idx = 0;
			for(int j=1;j<thumbnails.size();j++) {
				double d = Math.abs(thumbnails.get(j).avgColor.distanceFrom(refTiles[i].avgColor));
				if(d < distance) {
					distance = d;
					idx = j;
				}
			}
			refTiles[i] = thumbnails.get(idx);
		}
	}
	private boolean saveMozaic() throws IOException{
		if(mozaic == null) {
			return false;
		}
		File output;
		if(saveDestination.equals("")) {
			int n = 0;
			do {
				saveDestination = (refSrc.getParent()+"MozaicOutput"+((n<10)?"0"+n:n)+".jpg");
				output = new File(saveDestination);
				n++;
			}while (output.exists());
			output.createNewFile();
		}else {
			output = new File(saveDestination);
		}
		ImageIO.write(mozaic, "jpg", output);
		return true;
	}
	
	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("help", "Display the help menu");
		options.addOption("bnw", "Convert all images to black-and-white before processing.");
		options.addOption("t", "tolerance", true, "Set the tolerance for colors in the thumbnails. (0.00-1.00)");
		options.addOption("s", "save", true, "Set the save destination for the Mozaic. By default is set to the same location as the reference image.");
		
		DefaultParser parser = new DefaultParser();
		CommandLine cmdLn = null;
		try {
			cmdLn = parser.parse(options, args);
			for(Option opt:cmdLn.getOptions()) {
				//System.out.println("Flag "+opt.getOpt()+" with value of "+opt.getValue());
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			if(e instanceof UnrecognizedOptionException) {
				System.out.println(((UnrecognizedOptionException) e).getOption()+" is not a recognized flag");
				System.out.println("For a list of recognized flags, use \"-help\"");
			}
			e.printStackTrace();
		}
		if(cmdLn.hasOption("help")) {
			System.out.println("Image Mozaic Generator Help");
			for(Option option:options.getOptions()) {
				System.out.print("-"+option.getOpt());
				System.out.print("\t\t");
				System.out.println(option.getDescription());
			}
			//do not continue
			return;
		}
		if(cmdLn.hasOption("bnw")) {
			blackandwhite = true;
			System.out.println("Black and white enabled");
		}
		if(cmdLn.hasOption("t")) {
			marginthingy = Float.parseFloat(cmdLn.getOptionValue("t"));
			System.out.println("Margin thingy??? set to "+marginthingy);
		}
		if(cmdLn.hasOption("save")) {
			saveDestination = cmdLn.getOptionValue("save");
			System.out.println("Saving to "+saveDestination);
		}
		
		
		ImageMozaicGenerator mozaicGen = new ImageMozaicGenerator();
		
		String[] params = cmdLn.getArgs();
		try {
			mozaicGen.setReferenceImage(params[0]);
		} catch (IOException e) {
			System.out.println("Could not find reference image at");
			System.out.println(params[0]);
		}
		
		for(int i=1;i<params.length;i++) {
			//first parameter must be the reference image
			//all following ones are thumbnails
			
			try {
				mozaicGen.addThumbnails(params[i]);
			} catch (IOException e) {
				System.out.println("Could not find file at");
				System.out.println(params[i]);
			}
		}
		mozaicGen.generateMozaic();
		try {
			mozaicGen.saveMozaic();
		} catch (IOException e) {
			System.out.println("Could not save image at");
			System.out.println(saveDestination);
		}
		
		
	}
	class Thumbnail{
		BufferedImage img;
		String src;
		
		Color avgColor;
		public Thumbnail(File src) throws IOException{
			this.src = src.getAbsolutePath();
			img = Scalr.resize(ImageIO.read(src), 50);
			//brightness = getBrightness();
			//for each pixel in img, get Color (new Color(img.getRGB(x,y));)
			//red = color.getRed, etc
			//redAvg = avg(red from all pixels)
			
		}
		protected Thumbnail(BufferedImage img) {
			this.img = img;
			avgColor = getColorAvg(1.0f);
			//brightness = getBrightness();
		}
		public int getWidth() {
			return img.getWidth();
		}
		public int getHeight() {
			return img.getHeight();
		}
		public void scaleImage(int width, int height) {
			img = Scalr.resize(img, width, height, new BufferedImageOp[0]);
		}
		public void getAverageColor(float tolerance) {
			avgColor = getColorAvg(tolerance);
		}
		private Color getColorAvg(float tolerance) {
			int redAvg = 0;
			int greenAvg = 0;
			int blueAvg = 0;
			
			List<Integer> redValues = new ArrayList<Integer>();
			List<Integer> greenValues = new ArrayList<Integer>();
			List<Integer> blueValues = new ArrayList<Integer>();
			
			for(int h=0;h<img.getHeight();h++) {
				for(int w=0;w<img.getWidth();w++) {
					Color c = new Color(img.getRGB(w,h));
					redValues.add(c.getRed());
					greenValues.add(c.getGreen());
					blueValues.add(c.getBlue());
				}
			}
			redValues = getTopPercentage(redValues, tolerance);
			greenValues = getTopPercentage(greenValues, tolerance);
			blueValues = getTopPercentage(blueValues, tolerance);
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
			
			
			return new Color(redAvg, greenAvg, blueAvg);
		}
		private List<Integer> getTopPercentage(List<Integer> i, float tolerance) {
			i.sort(null);
			Collections.reverse(i);
			int numfrompercent = Math.round(i.size() * tolerance);
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
