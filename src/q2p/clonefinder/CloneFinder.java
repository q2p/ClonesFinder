package q2p.clonefinder;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import javax.imageio.ImageIO;

public final class CloneFinder {
	private static final String MAIN_FOLDER;
	private static final String PROG_FOLDER;
	private static final String IN_FOLDER;
	private static final String OUT_FOLDER;
	
	private static File[] files;
	private static int[] filesSizes;
	private static int[][] filesResolutions;
	private static int filesLeft;
	
	private static int feed = 0;
	
	private static boolean comparePixels;
	private static boolean searchInTo;
	
	private static final BufferedReader READER = new BufferedReader(new InputStreamReader(System.in));

	static {
		final String path = new File("test.file").getAbsolutePath().replace("\\", "/");
		MAIN_FOLDER = path.substring(0, path.indexOf("/p/")+3);
		PROG_FOLDER = path.substring(0, path.lastIndexOf("/")+1);
		IN_FOLDER = PROG_FOLDER+"in/";
		OUT_FOLDER = PROG_FOLDER+"out/";
	}
	
	public static final int readNumber(final int maxNumber, final String onWrongInput) {
		while(true) {
			try {
				int option = Integer.parseInt(READER.readLine());
				if(option > 0 && option <= maxNumber)
					return option;
			} catch(Exception e) {}
			System.out.println(onWrongInput);
		}
	}
	
	public static final void main(final String[] args) throws Exception {
		System.out.println("1) Compare binary");
		System.out.println("2) Compare pixels");
		comparePixels = (readNumber(2, "Invalid command") == 2);
		System.out.println("1) Search in \"P\" folder");
		System.out.println("2) Search in refered folder");
		searchInTo = (readNumber(2, "Invalid command") == 2);
		
		if(searchInTo)
			System.out.println("Enter path to folder:");
		final String searchInFolder = searchInTo?READER.readLine():MAIN_FOLDER;

		files = new File(IN_FOLDER).listFiles();
		filesLeft = files.length;
		if(comparePixels)
			loadPixels();
		else
			loadBinary();

		if(filesLeft != 0)
			searchIn(new File(searchInFolder));

		System.out.println("Search Ended.");
	}

	private static final void loadPixels() throws Exception {
		filesResolutions = new int[files.length][2];
		BufferedImage img = null;
		for(int i = files.length-1; i != -1; i--) {
			img = memorySafeImageLoad(files[i]);
			filesResolutions[i][0] = img.getWidth();
			filesResolutions[i][1] = img.getHeight();
		}
	}
	
	private static final BufferedImage memorySafeImageLoad(final File file) {
		if(!file.isFile())
			return null;
		while(true) {
			try {
				return ImageIO.read(file);
			} catch (final OutOfMemoryError e) {
				System.gc();
				System.out.println("Out of memory. Sleeping for 10 secounds.");
				try {
					Thread.sleep(10000);
				} catch(final InterruptedException e1) {}
			} catch(final Exception e) {
				return null;
			}
		}
	}

	private static final void loadBinary() throws IOException {
		filesSizes = new int[files.length];
		FileInputStream fis = null;
		for(int i = files.length-1; i != -1; i--) {
			fis = new FileInputStream(files[i]);
			filesSizes[i] = fis.available();
			fis.close();
		}
	}

	private static final void compareByPixels(final File file) throws IOException {
		BufferedImage foundImg = memorySafeImageLoad(file);
		if(foundImg == null)
			return;
		
		final int[] resolutionFound = new int[] {foundImg.getWidth(), foundImg.getHeight()};
		
		foundImg = null;
		
		for(int i = files.length-1; i != -1; i--) {
			if(filesResolutions[i][0] == resolutionFound[0] && filesResolutions[i][1] == resolutionFound[1] && !files[i].getName().equals(file.getName())) {
				foundImg = memorySafeImageLoad(file);
				
				BufferedImage sourceImg = memorySafeImageLoad(files[i]);
				
				if(sourceImg == null || foundImg == null)
					return;
				
				boolean different = false;
				
				for(int y = resolutionFound[1] - 1; y != -1; y--) {
					for(int x = resolutionFound[0] - 1; x != -1; x--) {
						if(foundImg.getRGB(x, y) != sourceImg.getRGB(x, y)) {
							different = true;
							break;
						}
					}
					if(different)
						break;
				}
				
				sourceImg = null;
				foundImg = null;
				
				if(!different)
					moveFile(file);
				if(filesLeft == 0)
					return;
			}
		}
	}

	private static final void compareByBinary(final File file) throws IOException {
		FileInputStream fisFound = new FileInputStream(file);
		
		final int size = fisFound.available();
		
		fisFound.close();

		int sz = 0;
		
		for(int i = files.length-1; i != -1; i--) {
			if(filesSizes[i] == size && !files[i].getName().equals(file.getName())) {
				fisFound = new FileInputStream(file);
				FileInputStream fisInput = new FileInputStream(files[i]);

				byte[] buffFound = new byte[1024];
				byte[] buffInput = new byte[1024];
				
				while(fisInput.available() > 0) {
					sz = fisFound.read(buffFound)-1;
					fisInput.read(buffInput);
					
					while(sz != -1) {
						if(buffFound[sz] != buffInput[sz]) {
							sz = -2;
							break;
						}
						sz--;
					}
					
					if(sz == -2)
						break;
				}
				fisFound.close();
				fisInput.close();
				
				buffFound = null;
				buffInput = null;
				
				if(sz != -2)
					moveFile(file);
				if(filesLeft == 0)
					return;
			}
		}
	}

	private static final void moveFile(final File file) throws IOException {
		final byte[] buff = new byte[1024];
		FileInputStream fis = new FileInputStream(file);
		final FileOutputStream fos = new FileOutputStream(OUT_FOLDER+file.getName());
		int readed;
		while(fis.available() > 0) {
			readed = fis.read(buff);
			fos.write(buff, 0, readed);
			fos.flush();
		}
		fos.close();
		fis.close();
		filesLeft--;
	}

	private static final void searchIn(final File start) throws IOException {
		final LinkedList<File> queue = new LinkedList<File>();
		
		queue.add(start);
		
		while(!queue.isEmpty()) {
			final File file = queue.removeLast();
			
			if(file.isFile()) {
				if(feed == 127)
					feed = 0;
				else
					feed++;
				
				if(feed == 0)
					System.out.println(file.getAbsolutePath());
				
				if(comparePixels)
					compareByPixels(file);
				else
					compareByBinary(file);

				if(filesLeft == 0)
					return;
			} else {
				final File[] files = file.listFiles();
				
				for(final File f : files)
					queue.addLast(f);
			}
		}
	}
}
