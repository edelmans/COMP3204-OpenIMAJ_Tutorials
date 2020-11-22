package uk.ac.soton.ecs.ae2u16;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.typography.hershey.HersheyFont;

/**
 * OpenIMAJ Hello world!
 *
 */
public class Ch1 {
    public static void main( String[] args ) {
    	//Create an image
        MBFImage image = new MBFImage(320,70, ColourSpace.RGB);

        //Fill the image with white
        image.fill(RGBColour.WHITE);
        		        
        //Render some test into the image
        // -- EXERCISE 1.2.1. Changing text and playing with the sample application
        image.drawText("The very first", 10, 60, HersheyFont.FUTURA_MEDIUM, 40, RGBColour.RED);

        //Apply a Gaussian blur
        image.processInplace(new FGaussianConvolve(1f));
        
        //Display the image
        DisplayUtilities.display(image);
    }
}
