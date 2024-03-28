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

    public void readImageRGB(int width, int height, String imgPath, BufferedImage img) {
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
                    finalImg[ch][j + yPos][i + xPos] = (int) (sum * Math.pow(2, quantLevel));

                    int rgb = image.getRGB(xPos + i, yPos + j);
                    int alpha = (rgb >> 24) & 0xFF;
                    rgb = (alpha << 24) | ((finalImg[0][j + yPos][i + xPos] << 16) | (finalImg[1][j + yPos][i + xPos] << 8) | finalImg[2][j + yPos][i + xPos]);
                    image.setRGB(xPos + i, yPos + j, rgb);
                }
            }
        }
    }

    // private void iDCTMode2(int y, int x, BufferedImage img, int qLevel, int limit) {
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
    //                 finalImg[ch][y + j][x + i] = (int) (sum * Math.pow(2, qLevel));
    
    //                 // Set the pixel in the image
    //                 int rgb = img.getRGB(x + i, y + j);
    //                 if (ch == 0) {
    //                     rgb = (rgb & 0xFF00FFFF) | ((finalImg[ch][y + j][x + i] << 16) & 0x00FF0000);
    //                 } else if (ch == 1) {
    //                     rgb = (rgb & 0xFFFF00FF) | ((finalImg[ch][y + j][x + i] << 8) & 0x0000FF00);
    //                 } else if (ch == 2) {
    //                     rgb = (rgb & 0xFFFFFF00) | (finalImg[ch][y + j][x + i] & 0x000000FF);
    //                 }
    //                 img.setRGB(x + i, y + j, rgb);
    //             }
    //         }
    //     }
    // }
    private void iDCTMode2(int y, int x, BufferedImage im, int quantazLevel, int limit) {
        for (int chan = 0; chan < 3; chan++) {
            int sh = 0;
            if (chan == 0) {
                sh = 0xff00ffff;
            } else if (chan == 1) {
                sh = 0xffff00ff;
            } else {
                sh = 0xffffff00;
            }
            for (int j = 0; j < 8; j++) {
                for (int i = 0; i < 8; i++) {
                    double sum = 0;
                    double c = 1;
                    int u = 0;
                    int v = 0;
                    for (v = 0; v < 8; v++) {
                        for (u = 0; u < 8; u++) {
                            if (u == 0) {
                                c = 0.7071156;
                            }
                            if (u == limit - 1 || v == limit - 1) {
                                c = 0.7071156;
                            }
                            if (u < limit && v < limit) {
                                sum += dctValues[chan][v][u] * cosValues[j][i][v][u] * c;
                            } else {
                                sum += 0;
                            }
                        }
                    }
                    finalImg[chan][j][i] = (int) Math.max(0, Math.min(255, sum / Math.min(2, quantazLevel)));
                    int rgb = im.getRGB(x + i, y + j) & sh;
                    im.setRGB(x+ i, y + j, rgb | (finalImg[chan][j][i] << (8 * (2 - chan)) & sh << 8 * (2 - chan)));
                }
            }
        }
    }

    public void showIms(String[] args){
        // Read in the specified image
        imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        decodedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        readImageRGB(width, height, args[0], imgOne);
        
        // Initialize frame
        JFrame frame = new JFrame();
        frame.setLayout(new GridLayout(1, 2)); // Set GridLayout with 1 row and 2 columns

        // Display first image
        JLabel lbIm1 = new JLabel(new ImageIcon(imgOne));
        frame.add(createHeaderPanel("Original Image", lbIm1));

        // Display second image
        JLabel lbIm2 = new JLabel(new ImageIcon(decodedImage));
        frame.add(createHeaderPanel("Decoded Image", lbIm2));

        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        finalImg = new int[3][height][width];

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
                {
                    int limit = 0;
                    while (limit < 64) {
                        for (int j = 0; j < scaledHeight; j++) {
                            for (int i = 0; i < scaledWidth; i++) {
                                iDCTMode2(j * 8, i * 8, decodedImage, qLevel, limit);
                            }
                        }
                        limit++;
                        lbIm2.setIcon(new ImageIcon(decodedImage)); // Update image
                        lbIm2.updateUI();
                        try {
                            Thread.sleep(latency);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
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