import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;


public class ImageDisplay {

	JFrame frame, frame2;
	JLabel lbIm1, lbIm2;
	BufferedImage imgOne;
    private static final int BLOCK_SIZE = 8;
    public static BufferedImage decodedImage;

	// Modify the height and width values here to read and display an image with
  	// different dimensions. 
	int width = 512;
	int height = 512;

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img)
	{
		try
		{
			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			int ind = 0;
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					// byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2]; 

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					img.setRGB(x,y,pix);
					ind++;
				}
			}
            raf.close();
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

    public static void encodeImage(BufferedImage image, int width, int height, int quantizationLevel) {
        int numBlocksX = width / BLOCK_SIZE;
        int numBlocksY = height / BLOCK_SIZE;

        // Iterate through each block
        for (int y = 0; y < numBlocksY; y++) {
            for (int x = 0; x < numBlocksX; x++) {
                // Extract block from image
                int[][][] block = extractBlock(image, x * BLOCK_SIZE, y * BLOCK_SIZE);

                // Apply DCT
                double[][][] dctCoefficients = new double[3][8][8];
                performDCT(block, dctCoefficients);
                
                int[][][] block1 = new int[3][8][8];
                performInverseDCT(dctCoefficients, block1);

                // Place block in decoded image
                placeBlock(decodedImage, block1, x * BLOCK_SIZE, y * BLOCK_SIZE);

                // Quantize DCT coefficients
                //int[][][] quantizedCoefficients = quantizeCoefficients(dctCoefficients, quantizationLevel);

                // Store quantized coefficients
                //encodedImage[y][x] = dctCoefficients; // Only store one color channel for simplicity
            }
        }

        // return encodedImage;
    }

    private static int[][][] extractBlock(BufferedImage image, int startX, int startY) {
        int[][][] block = new int[3][BLOCK_SIZE][BLOCK_SIZE];

        for (int y = 0; y < BLOCK_SIZE; y++) {
            for (int x = 0; x < BLOCK_SIZE; x++) {
                int rgb = image.getRGB(startX + x, startY + y);
                block[0][y][x] = (rgb >> 16) & 0xFF; // Red channel
                block[1][y][x] = (rgb >> 8) & 0xFF; // Green channel
                block[2][y][x] = rgb & 0xFF; // Blue channel
            }
        }

        return block;
    }

    // private static int[][][] performDCT(int[][][] block) {
    //     int[][][] dctResult = new int[block.length][8][8];
    
    //     for (int c = 0; c < block.length; c++) { // For each color channel
    //         for (int u = 0; u < 8; u++) {
    //             for (int v = 0; v < 8; v++) {
    //                 double sum = 0.0;
    //                 for (int x = 0; x < 8; x++) {
    //                     for (int y = 0; y < 8; y++) {
    //                         sum += block[c][x][y] * Math.cos((2 * x + 1) * u * Math.PI / 16.0)
    //                                 * Math.cos((2 * y + 1) * v * Math.PI / 16.0);
    //                     }
    //                 }
    //                 double alphaU = (u == 0) ? (1 / Math.sqrt(2)) : 1;
    //                 double alphaV = (v == 0) ? (1 / Math.sqrt(2)) : 1;
    //                 double cu = (u == 0) ? (1 / Math.sqrt(2)) : 1;
    //                 double cv = (v == 0) ? (1 / Math.sqrt(2)) : 1;
    //                 sum *= (0.25 * alphaU * alphaV * cu * cv);
    //                 dctResult[c][u][v] = (int) Math.round(sum);
    //             }
    //         }
    //     }
    
    //     return dctResult;
    // }
    
    // private static int[][][] quantizeCoefficients(int[][][] dctCoefficients, int quantizationLevel) {
    //     int[][][] quantizedCoefficients = new int[dctCoefficients.length][8][8];
    //     int quantizationFactor = 1 << quantizationLevel; // Equivalent to 2^quantizationLevel
    
    //     for (int c = 0; c < dctCoefficients.length; c++) { // For each color channel
    //         for (int u = 0; u < 8; u++) {
    //             for (int v = 0; v < 8; v++) {
    //                 quantizedCoefficients[c][u][v] = Math.round(dctCoefficients[c][u][v] / (float) quantizationFactor);
    //             }
    //         }
    //     }
    
    //     return quantizedCoefficients;
    // }    


    // public static BufferedImage decodeImage(int[][][] encodedImage, int width, int height, int quantizationLevel) {
    //     int numBlocksX = width / BLOCK_SIZE;
    //     int numBlocksY = height / BLOCK_SIZE;


    //     // Iterate through each block
    //     for (int y = 0; y < numBlocksY; y++) {
    //         for (int x = 0; x < numBlocksX; x++) {
    //             // Dequantize DCT coefficients
    //             int[][][] dequantizedCoefficients = encodedImage[y][x];//dequantizeCoefficients(encodedImage[y][x], quantizationLevel);

    //             // Apply inverse DCT
    //             int[][][] block = performInverseDCT(dequantizedCoefficients);

    //             // Place block in decoded image
    //             placeBlock(decodedImage, block, x * BLOCK_SIZE, y * BLOCK_SIZE);
    //         }
    //     }

    //     return decodedImage;
    // }

    // private static int[][][] dequantizeCoefficients(int[][][] quantizedCoefficients, int quantizationLevel) {
    //     int[][][] dequantizedCoefficients = new int[quantizedCoefficients.length][8][8];
    //     int quantizationFactor = 1 << quantizationLevel; // Equivalent to 2^quantizationLevel
    
    //     for (int c = 0; c < quantizedCoefficients.length; c++) { // For each color channel
    //         for (int u = 0; u < 8; u++) {
    //             for (int v = 0; v < 8; v++) {
    //                 dequantizedCoefficients[c][u][v] = quantizedCoefficients[c][u][v] * quantizationFactor;
    //             }
    //         }
    //     }
    
    //     return dequantizedCoefficients;
    // }

    // private static int[][][] performInverseDCT(int[][][] dctCoefficients) {
    //     int[][][] imageBlocks = new int[dctCoefficients.length][8][8];
    
    //     for (int c = 0; c < dctCoefficients.length; c++) { // For each color channel
    //         for (int x = 0; x < 8; x++) {
    //             for (int y = 0; y < 8; y++) {
    //                 double sum = 0.0;
    //                 for (int u = 0; u < 8; u++) {
    //                     for (int v = 0; v < 8; v++) {
    //                         double alphaU = (u == 0) ? (1 / Math.sqrt(2)) : 1;
    //                         double alphaV = (v == 0) ? (1 / Math.sqrt(2)) : 1;
    //                         double cu = (u == 0) ? (1 / Math.sqrt(2)) : 1;
    //                         double cv = (v == 0) ? (1 / Math.sqrt(2)) : 1;
    //                         double cosTerm1 = Math.cos((2 * x + 1) * u * Math.PI / 16.0);
    //                         double cosTerm2 = Math.cos((2 * y + 1) * v * Math.PI / 16.0);
    //                         sum += alphaU * alphaV * cu * cv * dctCoefficients[c][u][v] * cosTerm1 * cosTerm2;
    //                     }
    //                 }
    //                 sum *= 0.25;
    //                 imageBlocks[c][x][y] = (int) Math.round(sum);
    //             }
    //         }
    //     }
    
    //     return imageBlocks;
    // }


    private static void performDCT(int[][][] block, double[][][] dctCoefficients) {

        for (int c = 0; c < block.length; c++) { // For each color channel
            for (int u = 0; u < 8; u++) {
                for (int v = 0; v < 8; v++) {
                    double t = 0.0;
                    for (int x = 0; x < 8; x++) {
                        for (int y = 0; y < 8; y++) {
                            double cv = (v == 0) ? (1 / Math.sqrt(2)) : 1;
                            double cu = (u == 0) ? (1 / Math.sqrt(2)) : 1;
                            t += cu * cv * block[c][x][y] * Math.cos((2 * x + 1) * u * Math.PI / 16.0)
                                    * Math.cos((2 * y + 1) * v * Math.PI / 16.0);
                        }
                    }
                    t *= 0.25;
                    dctCoefficients[c][u][v] = t;
                }
            }
        }
    }
    
    private static void performInverseDCT(double[][][] dctCoefficients, int[][][] block) {
    
        for (int c = 0; c < dctCoefficients.length; c++) { // For each color channel
            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    double t = 0.0;
                    for (int u = 0; u < 8; u++) {
                        for (int v = 0; v < 8; v++) {
                            double cu = (u == 0) ? (1 / Math.sqrt(2)) : 1;
                            double cv = (v == 0) ? (1 / Math.sqrt(2)) : 1;
                            t += cu * cv * dctCoefficients[c][u][v] * Math.cos((2 * x + 1) * u * Math.PI / 16.0)
                                    * Math.cos((2 * y + 1) * v * Math.PI / 16.0);
                        }
                    }
                    t *= 0.25;
                    block[c][x][y] = (int) Math.round(t);
                }
            }
        }
    }
    

    private static void placeBlock(BufferedImage image, int[][][] block, int startX, int startY) {
        for (int y = 0; y < BLOCK_SIZE; y++) {
            for (int x = 0; x < BLOCK_SIZE; x++) {
                int rgb = (block[0][y][x] << 16) | (block[1][y][x] << 8) | block[2][y][x];
                image.setRGB(startX + x, startY + y, rgb);
            }
        }
    }

	public void showIms(String[] args){

		// Read in the specified image
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        decodedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, args[0], imgOne);

		// Use label to display the image
		frame = new JFrame();
		frame2 = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);
		frame2.getContentPane().setLayout(gLayout);

        encodeImage(imgOne, width, height, 100);


        BufferedImage quantizedImg= decodedImage;


		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;

        lbIm1 = new JLabel(new ImageIcon(quantizedImg));
        frame.getContentPane().add(createHeaderPanel("Uniform Quantization", lbIm1), c);
        // lbIm1 = new JLabel(new ImageIcon(quantizedImg));
        // frame.getContentPane().add(createHeaderPanel("Non - Uniform Quantization", lbIm1), c);

		frame.pack();
		frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

    private static JPanel createHeaderPanel(String headerText, JLabel contentLabel) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel headerLabel = new JLabel(headerText);
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(headerLabel, BorderLayout.NORTH);
        panel.add(contentLabel, BorderLayout.CENTER);
        return panel;
    }

	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}

}