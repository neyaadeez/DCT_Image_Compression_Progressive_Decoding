import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

public class ImageDisplay {

    JFrame frame;
    JLabel lbIm1;
    JLabel lbIm2;
    BufferedImage imageOne;
    public static BufferedImage decodedImage;

    int width = 512;
    int height = 512;
    int scaledHeight = height / 8;
    int scaledWidth = width / 8;
    int[][][] originalimage;
    int[][][] dctValues;
    double[][][][] cosValues;
    int[][][] finalimage;

    public void readImageRGB(int width, int height, String imagePath, BufferedImage image) {
        try {
            int frameLength = width * height * 3;
            originalimage = new int[3][height][width];

            File file = new File(imagePath);
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
                    originalimage[0][y][x] = r & 0xff;
                    originalimage[1][y][x] = g & 0xff;
                    originalimage[2][y][x] = b & 0xff;

                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    image.setRGB(x, y, pix);
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

    public void performDCT(int quantLevel) {
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
                                    sumDCT += originalimage[ch][j + y * 8][i + x * 8] * cosValues[v][u][j][i];
                                }
                            }
                            dctValues[ch][v + y * 8][u + x * 8] = (int) (sumDCT * c / Math.pow(2, quantLevel));
                        }
                    }
                }
            }
        }
    }

    public void iDCTMode1(int yPos, int xPos, BufferedImage image, int quantLevel) {
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
                    finalimage[ch][j + yPos][i + xPos] = (int) (sum * Math.pow(2, quantLevel));

                    int rgb = image.getRGB(xPos + i, yPos + j);
                    int alpha = (rgb >> 24) & 0xFF;
                    rgb = (alpha << 24) | ((finalimage[0][j + yPos][i + xPos] << 16) | (finalimage[1][j + yPos][i + xPos] << 8) | finalimage[2][j + yPos][i + xPos]);
                    image.setRGB(xPos + i, yPos + j, rgb);
                }
            }
        }
    }

    // private void iDCTMode2(int y, int x, BufferedImage image, int qLevel, int limit) {
    //     for (int ch = 0; ch < 3; ch++) {
    //         for (int j = 0; j < 8; j++) {
    //             for (int i = 0; i < 8; i++) {
    //                 double sum = 0;
    //                 double c;
    //                 for (int v = 0; v < 8; v++) {
    //                     for (int u = 0; u < 8; u++) {
    //                         if (u != 0 && v != 0)
    //                             c = 0.25;
    //                         else if ((u == 0 && v != 0) || (u != 0 && v == 0))
    //                             c = 0.25 * 0.707;
    //                         else
    //                             c = 0.125;
    
    //                         if ((u + v) <= limit) {
    //                             sum += dctValues[ch][y + v][x + u] * cosValues[v][u][j][i] * c;
    //                         }
    //                     }
    //                 }
    //                 finalimage[ch][y + j][x + i] = (int) (sum * Math.pow(2, qLevel));
    
    //                 // Set the pixel in the image
    //                 int rgb = image.getRGB(x + i, y + j);
    //                 if (ch == 0) {
    //                     rgb = (rgb & 0xFF00FFFF) | ((finalimage[ch][y + j][x + i] << 16) & 0x00FF0000);
    //                 } else if (ch == 1) {
    //                     rgb = (rgb & 0xFFFF00FF) | ((finalimage[ch][y + j][x + i] << 8) & 0x0000FF00);
    //                 } else if (ch == 2) {
    //                     rgb = (rgb & 0xFFFFFF00) | (finalimage[ch][y + j][x + i] & 0x000000FF);
    //                 }
    //                 image.setRGB(x + i, y + j, rgb);
    //             }
    //         }
    //     }
    // }
    

    public void iDCTProgressiveSpectral(int quantLevel, int latency, BufferedImage image) {
        for (int k = 0; k < 64; k++) {
            for(int z1=0;z1<scaledHeight;z1++)
            {
                for(int z2=0;z2<scaledWidth;z2++)
                {
            for (int ch = 0; ch < 3; ch++) {
                        for (int j = 0; j < 8; j++) {
                            for (int i = 0; i < 8; i++) {
                                double sum = 0;
                                double c;
                                for (int v = 0; v < 8; v++) {
                                    for (int u = 0; u < 8; u++) {
                                        if (u != 0 && v != 0)
                                            c = 0.25;
                                        else if ((u == 0 && v != 0) || (u != 0 && v == 0))
                                            c = 0.25 * 0.707;
                                        else
                                            c = 0.125;
                
                                        if ((u + v) <= k) {
                                            sum += dctValues[ch][z1*8 + v][z2*8 + u] * cosValues[v][u][j][i] * c;
                                        }
                                    }
                                }
                                finalimage[ch][z1*8 + j][z2*8 + i] = (int) (sum * Math.pow(2, quantLevel));
                
                                // Set the pixel in the image
                                int rgb = image.getRGB(z2*8 + i, z1*8 + j);
                                if (ch == 0) {
                                    rgb = (rgb & 0xFF00FFFF) | ((finalimage[ch][z1*8 + j][z2*8 + i] << 16) & 0x00FF0000);
                                } else if (ch == 1) {
                                    rgb = (rgb & 0xFFFF00FF) | ((finalimage[ch][z1*8 + j][z2*8 + i] << 8) & 0x0000FF00);
                                } else if (ch == 2) {
                                    rgb = (rgb & 0xFFFFFF00) | (finalimage[ch][z1*8 + j][z2*8 + i] & 0x000000FF);
                                }
                                image.setRGB(z2*8 + i, z1*8 + j, rgb);
                            }
                        }
                    }
                }
            }
            lbIm2.setIcon(new ImageIcon(image)); // Update image
            lbIm2.updateUI(); // Refresh UI
            try {
                Thread.sleep(latency);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    
    private BufferedImage createImageFromBlocks() {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int blockY = 0; blockY < scaledHeight; blockY++) {
            for (int blockX = 0; blockX < scaledWidth; blockX++) {
                for (int j = 0; j < 8; j++) {
                    for (int i = 0; i < 8; i++) {
                        int xPos = blockX * 8 + i;
                        int yPos = blockY * 8 + j;
                        int rgb = (finalimage[0][yPos][xPos] << 16) | (finalimage[1][yPos][xPos] << 8) | finalimage[2][yPos][xPos];
                        result.setRGB(xPos, yPos, rgb);
                    }
                }
            }
        }
        return result;
    }
    

    public void iDCTProgressiveBitApproximation(int quantLevel, int latency) {
        int maxBit = getMaxBit();
        for (int bit = 1; bit <= maxBit; bit++) {
            for (int ch = 0; ch < 3; ch++) {
                for (int blockY = 0; blockY < scaledHeight; blockY++) {
                    for (int blockX = 0; blockX < scaledWidth; blockX++) {
                        for (int j = 0; j < 8; j++) {
                            for (int i = 0; i < 8; i++) {
                                double sum = 0;
                                int count = 0;
                                int value = dctValues[ch][blockY * 8][blockX * 8];
                                while ((value & 1) == 0 && count < bit) {
                                    value >>= 1;
                                    count++;
                                }
                                if ((value & 1) == 1) { // Include coefficient if bit is significant
                                    for (int v = 0; v < 8; v++) {
                                        for (int u = 0; u < 8; u++) {
                                            double c;
                                            if (u != 0 && v != 0)
                                                c = 0.25;
                                            else if ((u == 0 && v != 0) || (u != 0 && v == 0))
                                                c = 0.25 * 0.707;
                                            else
                                                c = 0.125;
                                            sum += dctValues[ch][v + blockY * 8][u + blockX * 8] * cosValues[v][u][j][i] * c;
                                        }
                                    }
                                }
                                finalimage[ch][j + blockY * 8][i + blockX * 8] = (int) (sum * Math.pow(2, quantLevel));
                            }
                        }
                    }
                }
            }
            lbIm2.setIcon(new ImageIcon(createImageFromBlocks())); // Update image
            lbIm2.updateUI(); // Refresh UI
            try {
                Thread.sleep(latency);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    private int getMaxBit() {
        int maxBit = 0;
        for (int ch = 0; ch < 3; ch++) {
            for (int y = 0; y < scaledHeight; y++) {
                for (int x = 0; x < scaledWidth; x++) {
                    for (int v = 0; v < 8; v++) {
                        for (int u = 0; u < 8; u++) {
                            int dctValue = dctValues[ch][y * 8 + v][x * 8 + u];
                            int bit = getSignificantBit(dctValue);
                            if (bit > maxBit) {
                                maxBit = bit;
                            }
                        }
                    }
                }
            }
        }
        return maxBit;
    }
    
    private int getSignificantBit(int value) {
        int count = 0;
        while ((value & 1) == 0 && count < 32) {
            value >>= 1;
            count++;
        }
        return count;
    }
    
    private int getBit(int value, int index) {
        return (value >> index) & 1;
    }
    
    public void showIms(String[] args){
        // Read in the specified image
        imageOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        decodedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        readImageRGB(width, height, args[0], imageOne);
        
        // Initialize frame
        JFrame frame = new JFrame();
        frame.setLayout(new GridLayout(1, 2)); // Set GridLayout with 1 row and 2 columns

        // Display first image
        JLabel lbIm1 = new JLabel(new ImageIcon(imageOne));
        frame.add(createHeaderPanel("Original Image", lbIm1));

        // Display second image
        lbIm2 = new JLabel(new ImageIcon(decodedImage));
        frame.add(createHeaderPanel("Decoded Image", lbIm2));

        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        finalimage = new int[3][height][width];

        int qLevel = Integer.parseInt(args[1]);
        int mode = Integer.parseInt(args[2]);
        int latency = Integer.parseInt(args[3]);
        performDCT(qLevel);

        switch (mode) {
            case 1:
                if(latency != 0){
                    for (int j = 0; j < scaledHeight; j++) {
                        for (int i = 0; i < scaledWidth; i++) {
                            try {
                                iDCTMode1(j * 8, i * 8, decodedImage, qLevel);
                                lbIm2.setIcon(new ImageIcon(decodedImage)); // Update image
                                lbIm2.updateUI(); // Refresh UI
                                Thread.sleep(latency);
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }
                        }
                    }
                }else{
                    for (int j = 0; j < scaledHeight; j++) {
                        for (int i = 0; i < scaledWidth; i++) {
                                iDCTMode1(j * 8, i * 8, decodedImage, qLevel);
                        }
                    }
                    lbIm2.setIcon(new ImageIcon(decodedImage)); // Update image
                }
                break;
            case 2:
                iDCTProgressiveSpectral(qLevel, latency, decodedImage);
                break;
            case 3:
                iDCTProgressiveBitApproximation(qLevel, latency);
                break;


            default:
                break;
        }
    }

    public static JPanel createHeaderPanel(String headerText, JLabel contentLabel) {
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