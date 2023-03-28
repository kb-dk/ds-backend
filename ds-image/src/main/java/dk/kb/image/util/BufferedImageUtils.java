package dk.kb.image.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

public class BufferedImageUtils {

    /**
     *  This is just a temporary class to create a default no-access image
     */
    public static void main(String[] args) {

        JFrame frame= new JFrame();
        JLabel label=new JLabel();
        BufferedImage image = getNoAccessImage();

        frame.setTitle("Visning kun på stedet");
        frame.setSize(image.getWidth(), image.getHeight());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        label=new JLabel();
        label.setIcon(new ImageIcon(image));
        frame.getContentPane().add(label,BorderLayout.CENTER);
        frame.setLocationRelativeTo(null);
        frame.pack();
        frame.setVisible(true);                   
    }


    public static  BufferedImage getNoAccessImage() {
        final BufferedImage im = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
        drawTextInImg(im, "Visning kun på stedet", 10, 100);
        return im;
    }   

    private static void drawTextInImg(BufferedImage baseImage, String textToWrite, int x, int y) {
        Graphics2D g2D = (Graphics2D) baseImage.getGraphics();
        g2D.setColor(new Color(167, 136, 69));
        g2D.setFont(new Font("Comic Sans MS", Font.PLAIN, 26));
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2D.drawString(textToWrite, x, y);
        g2D.dispose();
    }
}
