package org.mosaic;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import org.listfunc.ListFunctions;

/*
 * The initial image is the "reference image"
 * It is divided into "tiles"
 * "Thumbnails" or "thumbs" are the small pictures
 * Which are substituted in to the tiles based on color matching
 * "Mosaic" is the final image
 */

/**
 * @author Casper
 *
 */
public class ImageMosaicGenerator {
	//The root directory for all files used in the program
	//If left blank, user must specify all file paths from root
	private static File rootDir;
	
	//File object for the reference image
	//Used to get the default save folder (parent of this file)
	private static File refSrc;
	
	//Reference Image
	private BufferedImage refImg;
	
	//Broken up tiles from the reference image, from left to right, and top to bottom
	private Thumbnail[] refTiles;
	
	//List containing the thumbnails to be used in the mosaic
	protected List<Thumbnail> thumbnails;
	
	//Image that will contain the final mosaic
	private BufferedImage mosaic;
	
	//The file in which the mosaic will be saved
	//If left empty, the default save folder (parent of reference image) is used, with a procedurally generated filename
	private static String saveDestination = "";
	
	//Determines what percentage of the most common color values are used when determining the average color
	private static float colorTolerance = 1.0f;
	
	//!!UNIMPLEMENTED!!
	//Whether all images should be made black and white before processing
	private static boolean blackandwhite = false;
	
	
	//The number of tiles the reference image will be divided into
	private static int xNumOfTiles = 50;
	private static int yNumOfTiles = 50;
	
	//The size of the thumbnails, defaults to the size of the reference  image divided by the number of tiles
	private static int thumbWidth;
	private static int thumbHeight;
	
	//How much to scale up the final mosaic in order to preserve the thumbnails
	private static float scaling = 1.0f;
	
	/**
	 * ImageMosaicGenerator Constructor
	 */
	public ImageMosaicGenerator(){
		//Initialize the thumbnails List as an ArrayList
		thumbnails = new ArrayList<Thumbnail>();
	}
	/**
	 * Set the reference image from a path String 
	 * 
	 * @param src String containing the file path for the image file
	 * @throws IOException Throws an exception if the file does not exist, or if the image cannot be read
	 */
	public void setReferenceImage(String src) throws IOException{
		refSrc = new File(rootDir, src);
		try {
			if(refSrc.getName().matches(".+?(\\.jpg|\\.jpeg|\\.png|\\.gif)$")) {
				refImg = ImageIO.read(refSrc);
			}else {
				throw new IOException();
			}
		}catch(IOException e) {
			throw new IOException(refSrc.getPath(), e);
		}
	}
	/**
	 * Essentially a convenience method that determines whether a path String is a File or Folder and passes it on accordingly
	 * 
	 * @param src String containing the file path for the image file, or folder containing the image files
	 * @throws IOException Throws an exception if the file does not exist, or if either of the contained methods throw an exception
	 */
	private void addThumbnails(String src) throws IOException {
		//Creates a file from the path string
		File file = new File(rootDir, src);
		if(!file.exists()) {
			throw new IOException(file.getPath());
		}
		//If the file is a directory, pass it to one method
		if(file.isDirectory()) {
			addThumbnailsFromDir(file);
		}else {
			//If the file is a file, pass it to the other
			if(file.isFile()) {
				addThumbnailsFromFile(file);
			}
		}
	}
	/**
	 * Adds a new image to the list of thumbnails
	 * 
	 * @param file Image file to be added
	 * @throws IOException Throws an exception if the file is not a readable image file
	 */
	protected void addThumbnailsFromFile(File file) throws IOException {
		//Tests that the file is an image file before adding it as a thumbnail
		if(file.getName().matches(".+?(\\.jpg|\\.jpeg|\\.png|\\.gif)$")) {
			thumbnails.add(new Thumbnail(file));
		}else {
			System.err.println(file.toString()+" is not a valid image file");
			throw new IOException(file.getPath());
		}
	}
	/**
	 * Adds all image files from a directory to the list of thumbnails
	 * 
	 * @param dir Directory to add all contained image files from
	 * @throws IOException Throws an exception if any file is not a readable image file
	 */
	protected void addThumbnailsFromDir(File dir) throws IOException {
		//Creates a filtered list of files contained in the directory
		File[] files = dir.listFiles(new FileFilter() {
			//Filter only (image) files with these extensions
			private final String[] acceptedExtensions = new String[] {".jpg", ".jpeg", ".png", ".gif"};
			
			//Only accept files that end with the accepted file extensions
			@Override
			public boolean accept(File file) {
				for(String ext : acceptedExtensions) {
					if(file.getName().toLowerCase().endsWith(ext)) {
						return true;
					}
				}
				return false;
			}
		});
		//Add all image files to the list as thumbnails
		for(File file:files) {
			try {
				thumbnails.add(new Thumbnail(file));
			}catch(IOException e) {
				throw new IOException(file.getPath(), e);
			}
		}
	}
	/**
	 * Main method of the program, generates the Mosaic and assigns the result to the mosaic variable.
	 */
	protected void generateMosaic() {
		if(blackandwhite == true) {
			//Convert the reference image to grayscale
			//Convert all thumbnails to grayscale
		}
		
		//The width and height of each tile, based on how many tiles must fit in the reference image
		int tileWidth = Math.round(refImg.getWidth()/xNumOfTiles);
		int tileHeight = Math.round(refImg.getHeight()/yNumOfTiles);
		if(tileWidth<1) {
			tileWidth = 1;
		}
		if(tileHeight<1) {
			tileHeight = 1;
		}
		
		
		//Get the tiles from the reference image
		refTiles = getRefTiles(tileWidth, tileHeight);
		
		/*
		* thumbWidth = Math.round(tileWidth*scaling);
		* thumbHeight = Math.round(tileHeight*scaling);
		*/
		
		//If the thumbnails do not already have a set width, set them to be the same size as the tiles
		if(thumbWidth < 1) {
			thumbWidth = tileWidth;
		}
		if(thumbHeight < 1) {
			thumbHeight = tileHeight;
		}
		
		//Scale the thumbnails based on the size of the tiles, and how much they should be scaled
		scaleThumbnails((thumbWidth), (thumbHeight));
		
		//Substitute each tile with the thumbnail that is closest in color
		//If the images are in grayscale, this is based on brightness
		substituteThumbnails();
		
		//Create an empty BufferedImage, then retrieve its graphics object
		mosaic = new BufferedImage(Math.round(xNumOfTiles*thumbWidth),Math.round(yNumOfTiles*thumbHeight),BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = mosaic.createGraphics();
		
		//Draw the thumbnails into the mosaic
		int i=0;
		for(int y=0;y<yNumOfTiles;y++) {
			for(int x=0;x<xNumOfTiles;x++) {
				try {
					g2d.drawImage(refTiles[i].img, null, Math.round(x*thumbWidth), Math.round(y*thumbHeight));
				}catch(Exception e) {
					e.printStackTrace();
				}
				i++;
			}
		}
	}
	/**
	 * Generates and retruns an array of tiles that compose the reference image,
	 * ordered from left to right, and top to bottom
	 * 
	 * @param width The width of each tile
	 * @param height The height of each tile
	 * @return Returns the array of tiles
	 */
	private Thumbnail[] getRefTiles(int width, int height) {
//		List<Thumbnail> tiles = new ArrayList<Thumbnail>();
		Thumbnail[] tiles = new Thumbnail[yNumOfTiles*xNumOfTiles];
		//Converts the reference image into subimages based on the width and height parameters and the number of x and y tiles requested
		int i = 0;
		for(int y=0;y<yNumOfTiles;y++) {
			for(int x=0;x<xNumOfTiles;x++) {
				try {
					tiles[i] = (new Thumbnail(refImg.getSubimage(x*width, y*height, width, height)));
					i++;
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		//Returns the list as an array, because I'm not really sure why not tbh
		//Should I just be using a list for conformity's sake? or do I use an array because less processing?
		
		//Thumbnail[] tileArray = new Thumbnail[tiles.size()];
		//return tiles.toArray(tileArray);
		return tiles;
	}
	/**
	 * Scales all thumbnails to the specified width and height, using the Thumbnail class' scaleImage method
	 * 
	 * @param width Width in pixels to scale the thumbnails
	 * @param height Height in pixels to scale the thumbnails
	 */
	private void scaleThumbnails(int width, int height) {
		for(Thumbnail thumbnail:thumbnails) {
			thumbnail.scaleImage(width, height);
		}
	}
	/**
	 * For each tile, replace it with the thumbnail that most closely matches its average color
	 */
	private void substituteThumbnails() {
		//Set the average color value in each thumbnail
		//This is only done at this stage because the images have already been compressed, so it takes less time to get the average color
		for(Thumbnail thumbnail:thumbnails) {
			thumbnail.getAverageColor(colorTolerance);
		}
		//For each tile, get the thumbnail that has an average color closest to the average color of the tile
		for(int i=0;i<refTiles.length;i++) {
			//Start with a value to compare the others with
			double distance = refTiles[i].avgColor.distanceFrom(thumbnails.get(0).avgColor);
			int idx = 0;
			//For each thumbnail, if the average color is closer than the last, set idx to its index
			for(int j=1;j<thumbnails.size();j++) {
				double d = refTiles[i].avgColor.distanceFrom(thumbnails.get(j).avgColor);
				if(d < distance) {
					distance = d;
					idx = j;
				}
			}
			//Replace the tile with the thumbnail that has the closest average color value
			refTiles[i] = thumbnails.get(idx);
		}
	}
	/**
	 * Saves the mosaic to the saveDestination if set. 
	 * If the save destination is not set, it saves to a file relative to the reference image
	 * 
	 * @return Returns false if the mosaic has not been created
	 * @throws IOException 
	 */
	private boolean saveMosaic() throws IOException{
		//If the mosaic was not created, something has gone very wrong.
		if(mosaic == null) {
			System.err.println("Mosaic not generated, contact support.");
			return false;
		}
		
		File output;
		//If no save destination was set, generate one in the same folder as the reference image
		if(saveDestination.equals("")) {
			//In order to avoid overwriting a file, append a number to the name of the file
			//If that file already exists, increment n
			//Loop until a filename is found that does not already exist
			int n = 0;
			do {
				output = new File(refSrc.getParent(), ("MosaicOutput"+((n<10)?"0"+n:n)+".jpg"));
				n++;
			}while (output.exists());
		}else {
			output = new File(rootDir, saveDestination);
		}
		output.createNewFile();
		System.out.println("Saving mosaic to "+output.getCanonicalPath());
		//Writes the mosaic image to the output file
		ImageIO.write(mosaic, "jpg", output);
		return true;
	}
	private static void printHelpMenu(Options options) {
		System.out.println("Image Mosaic Generator Help\n");
		System.out.println("Running this command-line program requires at least two arguments:");
		System.out.println("One image file to be used as a reference");
		System.out.println("One or more image files, or folders containing image files, to be used as tiles in the mosaic");
		for(Option option:options.getOptions()) {
			System.out.print("-"+option.getOpt());
			System.out.print("\t\t");
			System.out.println(option.getDescription());
		}
	}
	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("help", "Display the help menu");
		options.addOption("root", true, "Set the root directory for all files");
		options.addOption("save", true, "Set the save destination for the Mosaic. By default is set to the same location as the reference image.");
		
		options.addOption("w", "width", true, "Set the width of each thumbnail");
		options.addOption("h", "height", true, "Set the height of each thumbnail");
		
		options.addOption("x", true, "Set the number of horizontal tiles. (Default 50)");
		options.addOption("y", true, "Set the number of vertical tiles. (Default 50)");
		
		//options.addOption("scale", true, "Set the scaling for the final image");
		
		options.addOption("t", "tolerance", true, "Set the tolerance for colors in the thumbnails. (0.00-1.00)");
		
		options.addOption("bnw", "Convert all images to black-and-white before processing.");
		
		DefaultParser parser = new DefaultParser();
		CommandLine cmdLn = null;
		try {
			cmdLn = parser.parse(options, args);
		} catch (ParseException e) {
			if(e instanceof UnrecognizedOptionException) {
				System.err.println(((UnrecognizedOptionException) e).getOption()+" is not a recognized flag");
				System.err.println("For a list of recognized flags, use \"-help\"");
				System.exit(3);
			}
		}
		
		if(cmdLn.hasOption("help")) {
			printHelpMenu(options);
			//do not continue processing other flags
			return;
		}
		if(cmdLn.getArgs().length < 2) {
			printHelpMenu(options);
			//do not continue processing other flags
			return;
		}
		
		if(cmdLn.hasOption("root")) {
			rootDir = new File(cmdLn.getOptionValue("root"));
			System.out.println("Root folder set to "+rootDir);
			if(!rootDir.exists()||!rootDir.isDirectory()) {
				System.err.print("Could not find folder at ");
				System.err.println(rootDir);
				System.exit(1);
			}
		}
		
		if(cmdLn.hasOption("save")) {
			saveDestination = cmdLn.getOptionValue("save");
			System.out.println("Mosaic will be saved at "+saveDestination);
		}
		
		if(cmdLn.hasOption("w")) {
			try {
				thumbWidth = Integer.parseInt(cmdLn.getOptionValue("w"));
				System.out.println("Thumbnails will be "+thumbWidth+" pixels wide");
				if(thumbWidth<1) {
					throw new NumberFormatException();
				}
			}catch(NumberFormatException e) {
				System.err.println("Width value must be an integer greater than zero");
				System.exit(2);
			}
		}
		
		
		if(cmdLn.hasOption("h")) {
			try {
				thumbHeight = Integer.parseInt(cmdLn.getOptionValue("h"));
				System.out.println("Thumbnails will be "+thumbHeight+" pixels high");
				if(thumbHeight<1) {
					throw new NumberFormatException();
				}
			}catch(NumberFormatException e) {
				System.err.println("Height value must be an integer greater than zero");
				System.exit(2);
			}
		}
		
		
		if(cmdLn.hasOption("x")) {
			try {
				xNumOfTiles = Integer.parseInt(cmdLn.getOptionValue("x"));
				if(xNumOfTiles<1) {
					throw new NumberFormatException();
				}
			}catch(NumberFormatException e) {
				System.err.println("Width value must be an integer greater than zero");
				System.exit(2);
			}
		}
		System.out.println("Mozaic will be "+xNumOfTiles+" tiles wide");
		
		if(cmdLn.hasOption("y")) {
			try {
				yNumOfTiles = Integer.parseInt(cmdLn.getOptionValue("y"));
				if(yNumOfTiles<1) {
					throw new NumberFormatException();
				}
			}catch(NumberFormatException e) {
				System.err.println("Width value must be an integer greater than zero");
				System.exit(2);
			}
		}
		System.out.println("Mozaic will be "+yNumOfTiles+" tiles wide");
		/*
		* if(cmdLn.hasOption("scale")) {
		* 	try {
		*		scaling = Float.parseFloat(cmdLn.getOptionValue("scale"));
		*		if(scaling<=0) {
		*			throw new NumberFormatException();
		*		}
		*	}catch(NumberFormatException e) {
		*		System.err.println("Scaling value must be a floating point number greater than zero");
		*		System.exit(2);
		*	}
		*	System.out.println("Scaling image to "+scaling+"x");
		* }
		*/
		
		if(cmdLn.hasOption("t")) {
			colorTolerance = Float.parseFloat(cmdLn.getOptionValue("t"));
			System.out.println("Color tolerance set to "+colorTolerance);
			if(colorTolerance <= 0||colorTolerance >1) {
				System.err.println("Color tolerance must be between Zero (exclusive) and One (inclusive)");
				System.exit(1);
			}
		}
		
		if(cmdLn.hasOption("bnw")) {
			blackandwhite = true;
			System.out.println("Black and white enabled");
		}
		
		
		
		
		ImageMosaicGenerator mosaicGen = new ImageMosaicGenerator();
		
		String[] params = cmdLn.getArgs();
		try {
			mosaicGen.setReferenceImage(params[0]);
			System.out.println("Added reference image from "+params[0]);
		} catch (IOException e) {
			System.err.print("Could not find reference image at ");
			System.err.println(e.getMessage());
			System.exit(1);
		}
		
		for(int i=1;i<params.length;i++) {
			//first parameter must be the reference image
			//all following ones are thumbnails
			
			try {
				mosaicGen.addThumbnails(params[i]);
				System.out.println("Added thumbnail(s) from "+params[i]);
			} catch (IOException e) {
				System.err.print("Could not find file at ");
				System.err.println(e.getMessage());
			}
		}
		if(mosaicGen.thumbnails.size() < 1) {
			System.err.println("No valid thumbnails found");
			System.exit(1);
		}
		
		mosaicGen.generateMosaic();
		try {
			mosaicGen.saveMosaic();
		} catch (IOException e) {
			System.err.print("Could not save image at ");
			System.err.println(saveDestination);
		}
		
		
		
	}
	/**
	 * A class containing a BufferedImage object with extra methods to operate on it.
	 * 
	 * @author Arley Morris
	 */
	class Thumbnail{
		//The image represented by this object 
		private BufferedImage img;
		
		Color avgColor;
		public Thumbnail(File src) throws IOException{
			img = ImageIO.read(src);
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
		    Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
		    img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		    Graphics2D g2d = img.createGraphics();
		    g2d.drawImage(tmp, 0, 0, null);
		    g2d.dispose();
		    tmp.flush();
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
			redValues = ListFunctions.getTopPercentage(redValues, tolerance);
			greenValues = ListFunctions.getTopPercentage(greenValues, tolerance);
			blueValues = ListFunctions.getTopPercentage(blueValues, tolerance);
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
			return Math.abs(Math.sqrt(Math.pow(redDiff, 2)+Math.pow(greenDiff, 2)+Math.pow(blueDiff, 2)));
		}
	}
}
