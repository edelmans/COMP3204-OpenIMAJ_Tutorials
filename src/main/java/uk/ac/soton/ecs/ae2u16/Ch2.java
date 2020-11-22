package uk.ac.soton.ecs.ae2u16;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.processing.edges.CannyEdgeDetector;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.math.geometry.shape.Ellipse;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * OpenIMAJ Hello world!
 *
 */
public class Ch2 {
    public static void main( String[] args ) throws IOException {
        //  Read image from a file or a URL
        //MBFImage image = ImageUtilities.readMBF(new File("file.jpg"));
        MBFImage image = ImageUtilities.readMBF(new URL("http://static.openimaj.org/media/tutorial/sinaface.jpg"));

        //  -- EXERCISE 2.1.1. DisplayUtilities
        JFrame frame = DisplayUtilities.createNamedWindow("frame","2.1.1.", true);
        //  Every display() function has been modified by using the constructor with JFrame

        //  FImage is a greyscale image
        //  MBFImage is a multi-band version of FImage

        //  Print bands of the image
        System.out.println(image.colourSpace);

        //  Display image
        //DisplayUtilities.display(image);
        //  Display red channel
        DisplayUtilities.display(image.getBand(0), frame);

        //Set all images blue and green pixels to black
        MBFImage clone = image.clone();
//        for (int y=0; y<image.getHeight(); y++) {
//            for(int x=0; x<image.getWidth(); x++) {
//                clone.getBand(1).pixels[y][x] = 0;
//                clone.getBand(2).pixels[y][x] = 0;
//            }
//        }

        //  Code that does the same as before, but simpler
        clone.getBand(1).fill(0f);
        clone.getBand(2).fill(0f);

        DisplayUtilities.display(clone, frame);

        //  Apply canny edge detector to image
        image.processInplace(new CannyEdgeDetector());

        DisplayUtilities.display(image, frame);

        // Draw on an image in OpenIMAJ
        image.drawShapeFilled(new Ellipse(700f, 450f, 20f, 10f, 0f), RGBColour.WHITE);
        image.drawShapeFilled(new Ellipse(650f, 425f, 25f, 12f, 0f), RGBColour.WHITE);
        image.drawShapeFilled(new Ellipse(600f, 380f, 30f, 15f, 0f), RGBColour.WHITE);
        image.drawShapeFilled(new Ellipse(500f, 300f, 100f, 70f, 0f), RGBColour.WHITE);
        image.drawText("OpenIMAJ is", 425, 300, HersheyFont.ASTROLOGY, 20, RGBColour.BLACK);
        image.drawText("Awesome", 425, 330, HersheyFont.ASTROLOGY, 20, RGBColour.BLACK);
        DisplayUtilities.display(image, frame);

        //  -- EXERCISE 2.1.2. Drawing
        // The documentation provides drawShape(Shape s, int thickness, Q col)
        image.drawShape(new Ellipse(700f, 450f, 20f, 10f, 0f), 4, RGBColour.CYAN);
        image.drawShape(new Ellipse(650f, 425f, 25f, 12f, 0f), 4, RGBColour.CYAN);
        image.drawShape(new Ellipse(600f, 380f, 30f, 15f, 0f), 4, RGBColour.CYAN);
        image.drawShape(new Ellipse(500f, 300f, 100f, 70f, 0f),4, RGBColour.CYAN);
        DisplayUtilities.display(image,frame);

    }
}
