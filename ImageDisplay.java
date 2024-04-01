import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

public class ImageDisplay {

    JFrame frame;
    JLabel lbIm1;
    JLabel lbIm2;
    BufferedImage imageOne;
    BufferedImage decodedImage;

    int width = 512;
    int height = 512;
    int scaledHeight = height / 8;
    int scaledWidth = width / 8;
    int[][][] originalimage;
    int[][][] dctValues;
    double[][][][] cos;
    int[][][] finalimage;
    double c1 = 0.25;
    double c2 = 0.25 * 0.707;
    double c3 = 0.125;

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

    public void encodeDCT(int quantLevel) {
        double calpow = Math.pow(2, quantLevel);
        cos = new double[8][8][8][8];
        double pi = Math.PI;
        dctValues = new int[3][height][width];

        for (int x = 0; x < 8; x++) {
            int x2 = 2 * x + 1;
            for (int y = 0; y < 8; y++) {
                for (int a = 0; a < 8; a++) {
                    int a2 = 2 * a + 1;
                    for (int b = 0; b < 8; b++) {
                        cos[y][b][x][a] = Math.cos(pi * x2 * y * 0.0625) * Math.cos(pi * a2 * b * 0.0625);
                    }
                }
            }
        }        

        for (int ch = 0; ch < 3; ch++) {
            for (int y = 0; y < scaledHeight; y++) {
                for (int x = 0; x < scaledWidth; x++) {
                    double c;
                    for (int v = 0; v < 8; v++) {
                        for (int u = 0; u < 8; u++) {
                            double sumDCT = 0;
                            if (u != 0 && v != 0)
                                c = c1;
                            else if ((u == 0 && v != 0) || (u != 0 && v == 0))
                                c = c2;
                            else
                                c = c3;
                            for (int j = 0; j < 8; j++) {
                                for (int i = 0; i < 8; i++) {
                                    sumDCT += originalimage[ch][j + y * 8][i + x * 8] * cos[v][u][j][i];
                                }
                            }
                            dctValues[ch][v + y * 8][u + x * 8] = (int) (sumDCT * c / calpow);
                        }
                    }
                }
            }
        }
    }

    public void iDCTBaselineMode(int quantLevel, int latency, BufferedImage image) {
        double calpow = Math.pow(2, quantLevel);
        int flag = 1;
        if(latency == 0){
            flag = 0;
        }
        for(int z1=0;z1<scaledHeight;z1++){
            for(int z2=0;z2<scaledWidth;z2++){
                for (int ch = 0; ch < 3; ch++) {
                    for (int j = 0; j < 8; j++) {
                        for (int i = 0; i < 8; i++) {
                            double sum = 0;
                            for (int v = 0; v < 8; v++) {
                                for (int u = 0; u < 8; u++) {
                                    double c;
                                    if (u != 0 && v != 0)
                                        c = c1;
                                    else if ((u == 0 && v != 0) || (u != 0 && v == 0))
                                        c = c2;
                                    else
                                        c = c3;
                                    sum += dctValues[ch][z1*8 + v][z2*8 + u] * cos[v][u][j][i] * c;
                                }
                            }
                            finalimage[ch][z1*8 + j][z2*8 + i] = (int) (sum * calpow);
                
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
                if(flag == 1){
                    lbIm2.setIcon(new ImageIcon(image)); // Update image
                    lbIm2.updateUI(); // Refresh UI
                    try {
                        Thread.sleep(latency);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if(flag == 0){
            lbIm2.setIcon(new ImageIcon(image)); // Update image
            lbIm2.updateUI();
        }
    }

    
    public void iDCTProgressiveSpectral(int quantLevel, int latency, BufferedImage image) {
        double calpow = Math.pow(2, quantLevel);
        for (int k = 0; k < 64; k++) {
            for(int z1=0;z1<scaledHeight;z1++){
                for(int z2=0;z2<scaledWidth;z2++){
                    for (int ch = 0; ch < 3; ch++) {
                        for (int j = 0; j < 8; j++) {
                            for (int i = 0; i < 8; i++) {
                                double sum = 0;
                                double c;
                                int check = 0;
                                int v = 0, u = 0;
                                boolean goingUp = true;

                                while (v < 8 && u < 8) {
                                    if (u != 0 && v != 0)
                                        c = c1;
                                    else if ((u == 0 && v != 0) || (u != 0 && v == 0))
                                        c = c2;
                                    else
                                        c = c3;
                                    sum += dctValues[ch][z1*8 + v][z2*8 + u] * cos[v][u][j][i] * c;
                                    if(++check >= k)
                                        break;
                                    // If going up
                                    if (goingUp) {
                                        // If we are at the first row, move right
                                        if (v == 0) {
                                            u++;
                                            goingUp = false;
                                        }
                                        // If we are at the last column, move down
                                        else if (u == 8 - 1) {
                                            v++;
                                            goingUp = false;
                                        }
                                        // Move diagonally up-left
                                        else {
                                            v--;
                                            u++;
                                        }
                                    } else {
                                        // If we are at the last row, move down
                                        if (v == 8 - 1) {
                                            u++;
                                            goingUp = true;
                                        }
                                        // If we are at the first column, move right
                                        else if (u == 0) {
                                            v++;
                                            goingUp = true;
                                        }
                                        // Move diagonally down-right
                                        else {
                                            v++;
                                            u--;
                                        }
                                    }
                                }
                                finalimage[ch][z1*8 + j][z2*8 + i] = (int) (sum * calpow);
                
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
    

    public void progressiveMode(int quantLevel, int latency, BufferedImage image) {
        int maxBitDepth = 31;
        double calpow = Math.pow(2, quantLevel);
        for (int bit = 1; bit <= maxBitDepth; bit++) {
            int shiftAmount = maxBitDepth - bit;
            for (int z1 = 0; z1 < scaledHeight; z1++) {
                for (int z2 = 0; z2 < scaledWidth; z2++) {
                    for (int ch = 0; ch < 3; ch++) {
                        for (int j = 0; j < 8; j++) {
                            for (int i = 0; i < 8; i++) {
                                double sum = 0;
                                for (int v = 0; v < 8; v++) {
                                    for (int u = 0; u < 8; u++) {
                                        double c;
                                        if (u != 0 && v != 0)
                                            c = c1;
                                        else if ((u == 0 && v != 0) || (u != 0 && v == 0))
                                            c = c2;
                                        else
                                            c = c3;
                                        
                                        double coefficient = dctValues[ch][z1 * 8 + v][z2 * 8 + u];
                                        long coefficientAsLong = (long) coefficient;
                                        coefficientAsLong = (coefficientAsLong >> shiftAmount) << shiftAmount;
                                        coefficient = coefficientAsLong;
                                        
                                        sum += coefficient * cos[v][u][j][i] * c;
                                    }
                                }
                                finalimage[ch][z1 * 8 + j][z2 * 8 + i] = (int) (sum * calpow);
    
                                // Set the pixel in the image
                                int rgb = image.getRGB(z2 * 8 + i, z1 * 8 + j);
                                if (ch == 0) {
                                    rgb = (rgb & 0xFF00FFFF) | ((finalimage[ch][z1 * 8 + j][z2 * 8 + i] << 16) & 0x00FF0000);
                                } else if (ch == 1) {
                                    rgb = (rgb & 0xFFFF00FF) | ((finalimage[ch][z1 * 8 + j][z2 * 8 + i] << 8) & 0x0000FF00);
                                } else if (ch == 2) {
                                    rgb = (rgb & 0xFFFFFF00) | (finalimage[ch][z1 * 8 + j][z2 * 8 + i] & 0x000000FF);
                                }
                                image.setRGB(z2 * 8 + i, z1 * 8 + j, rgb);
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
        encodeDCT(qLevel);

        switch (mode) {
            case 1:
                iDCTBaselineMode(qLevel, latency, decodedImage);
                break;
            case 2:
                iDCTProgressiveSpectral(qLevel, latency, decodedImage);
                break;
            case 3:
                progressiveMode(qLevel, latency, decodedImage);
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