import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

public class ImageDisplay {

    JFrame frame;
    JLabel lbIm1;
    BufferedImage imgOne;
    public static BufferedImage decodedImage;

    int width = 512;
    int height = 512;
    int scaledHeight = height / 8;
    int scaledWidth = width / 8;
    int[][][] originalImg;
    int[][][] dctValues;
    double[][][][] cosValues;
    int[][][] finalImg;

    private void readImageRGB(int width, int height, String imgPath, BufferedImage img) {
        try {
            int frameLength = width * height * 3;
            originalImg = new int[3][height][width];

            File file = new File(imgPath);
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(0);

            long len = frameLength;
            byte[] bytes = new byte[(int) len];

            raf.read(bytes);

            int ind = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    byte r = bytes[ind];
                    byte g = bytes[ind + height * width];
                    byte b = bytes[ind + height * width * 2];
                    originalImg[0][y][x] = r & 0xff;
                    originalImg[1][y][x] = g & 0xff;
                    originalImg[2][y][x] = b & 0xff;

                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    img.setRGB(x, y, pix);
                    ind++;
                }
            }
            raf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void performDCT(int quantLevel) {
        cosValues = new double[8][8][8][8];
        double pi = Math.PI;
        dctValues = new int[3][height][width];

        for (int i1 = 0; i1 < 8; i1++) {
            int i1_2 = 2 * i1 + 1;
            for (int j1 = 0; j1 < 8; j1++) {
                for (int i2 = 0; i2 < 8; i2++) {
                    int i2_2 = 2 * i2 + 1;
                    for (int j2 = 0; j2 < 8; j2++) {
                        cosValues[j1][j2][i1][i2] = Math.cos(pi * i1_2 * j1 * 0.0625) * Math.cos(pi * i2_2 * j2 * 0.0625);
                    }
                }
            }
        }

        for (int ch = 0; ch < 3; ch++) {
            for (int y = 0; y < scaledHeight; y++) {
                for (int x = 0; x < scaledWidth; x++) {
                    for (int v = 0; v < 8; v++) {
                        for (int u = 0; u < 8; u++) {
                            double sumDCT = 0;
                            double c = 0;
                            if (u != 0 && v != 0)
                                c = 0.25;
                            else if ((u == 0 && v != 0) || (u != 0 && v == 0))
                                c = 0.25 * 0.707;
                            else
                                c = 0.125;

                            for (int j = 0; j < 8; j++) {
                                for (int i = 0; i < 8; i++) {
                                    sumDCT += originalImg[ch][j + y * 8][i + x * 8] * cosValues[v][u][j][i];
                                }
                            }
                            dctValues[ch][v + y * 8][u + x * 8] = (int) (sumDCT * c / Math.pow(2, quantLevel));
                        }
                    }
                }
            }
        }
    }

    private void performInverseDCT(int yPos, int xPos, BufferedImage image, int quantLevel) {
        for (int ch = 0; ch < 3; ch++) {
            for (int j = 0; j < 8; j++) {
                for (int i = 0; i < 8; i++) {
                    double sum = 0;
                    for (int v = 0; v < 8; v++) {
                        for (int u = 0; u < 8; u++) {
                            double c;
                            if (u != 0 && v != 0)
                                c = 0.25;
                            else if ((u == 0 && v != 0) || (u != 0 && v == 0))
                                c = 0.25 * 0.707;
                            else
                                c = 0.125;

                            sum += dctValues[ch][v + yPos][u + xPos] * cosValues[v][u][j][i] * c;
                        }
                    }
                    finalImg[ch][j + yPos][i + xPos] = (int) (sum * Math.pow(2, quantLevel));

                    int rgb = image.getRGB(xPos + i, yPos + j);
                    int alpha = (rgb >> 24) & 0xFF;
                    rgb = (alpha << 24) | ((finalImg[0][j + yPos][i + xPos] << 16) | (finalImg[1][j + yPos][i + xPos] << 8) | finalImg[2][j + yPos][i + xPos]);
                    image.setRGB(xPos + i, yPos + j, rgb);
                }
            }
        }
    }

    public void showIms(String[] args){
        // Read in the specified image
        imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        decodedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        readImageRGB(width, height, args[0], imgOne);
        finalImg = new int[3][height][width];
    
        // Use label to display the image
        frame = new JFrame();
        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);
    
        int qLevel = Integer.parseInt(args[1]);
        int mode = Integer.parseInt(args[2]);
        int latency = Integer.parseInt(args[3]);
        performDCT(qLevel);
        switch (mode) {
            case 1:
                
                break;
        
            default:
                break;
        }

        for(int i=0; i<8; i++){
            for(int j=0; j<8; j++){
                System.out.print(originalImg[1][i][j]);
            }
            System.out.println();
        }
        System.out.println("---------------");

        for(int i=0; i<8; i++){
            for(int j=0; j<8; j++){
                System.out.print(dctValues[1][i][j]);
            }
            System.out.println();
        }
    
        // Perform inverse DCT on the entire image
        for(int j=0;j<scaledHeight;j++)
		{
		   	for(int i=0;i<scaledWidth;i++)
		   	{
		   		performInverseDCT(j*8,i*8,decodedImage,qLevel);
		   	}
		}

        BufferedImage quantizedImg = decodedImage;
    
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;

        // Display first image
        JLabel lbIm1 = new JLabel(new ImageIcon(imgOne));
        frame.getContentPane().add(createHeaderPanel("Original Image", lbIm1), c);

        c.gridx = 1; // Move to the next column

        // Display second image
        JLabel lbIm2 = new JLabel(new ImageIcon(quantizedImg));
        frame.getContentPane().add(createHeaderPanel("Baseline Mode", lbIm2), c);

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