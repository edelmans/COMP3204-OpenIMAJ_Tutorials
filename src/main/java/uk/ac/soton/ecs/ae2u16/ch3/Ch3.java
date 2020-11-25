package uk.ac.soton.ecs.ae2u16.ch3;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.connectedcomponent.GreyscaleConnectedComponentLabeler;
import org.openimaj.image.pixel.ConnectedComponent;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.processor.PixelProcessor;
import org.openimaj.image.segmentation.FelzenszwalbHuttenlocherSegmenter;
import org.openimaj.image.segmentation.SegmentationUtilities;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.ml.clustering.FloatCentroidsResult;
import org.openimaj.ml.clustering.assignment.HardAssigner;
import org.openimaj.ml.clustering.kmeans.FloatKMeans;

import javax.swing.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * OpenIMAJ Hello world!
 *
 */
public class Ch3 {
    public static void main( String[] args ) throws IOException {
        //Create a JFrame for display
        JFrame frame = DisplayUtilities.createNamedWindow("frame", "Chapter 3", true);

        //Read from URL
        MBFImage input = ImageUtilities.readMBF(new URL("https://images.unsplash.com/photo-1603815790321-5c56abc61547?ixlib=rb-1.2.1&ixid=MXwxMjA3fDB8MHxlZGl0b3JpYWwtZmVlZHw4MXx8fGVufDB8fHw%3D&auto=format&fit=crop&w=500&q=60"));
        //Part of 3.1.1 and 3.1.2. exercise, working on a clone before doing PixelProcessor and using Segmenter(see exercise below)
        MBFImage clone = input.clone();
        MBFImage segmentationClone = input.clone();


        //Transform image to an alternative colour space Lab
        input = ColourSpace.convert(input, ColourSpace.CIE_Lab);

        //Construct K-Means algorithm | createExact(int k) where k is number of clusters
        FloatKMeans cluster = FloatKMeans.createExact(2);

        //Flatten pixels of an image into the required form for FloatKMeans
        float[][] imageData = input.getPixelVectorNative(new float[input.getWidth() * input.getHeight()][3]);

        //Run K-Means algorithm
        FloatCentroidsResult result = cluster.cluster(imageData);

        //Print coordinates of each centroid
        final float[][] centroids = result.centroids;
        for (float[] fs : centroids) {
            System.out.println(Arrays.toString(fs));
        }

        //Use HardAssigner to assign each pixel to its class using the centroids above
        final HardAssigner<float[],?,?> assigner = result.defaultHardAssigner();
        for (int y=0; y<input.getHeight(); y++) {
            for (int x=0; x<input.getWidth(); x++) {
                float[] pixel = input.getPixelNative(x, y);
                int centroid = assigner.assign(pixel);
                input.setPixelNative(x, y, centroids[centroid]);
            }
        }

        //Convert image back to RGB before displaying it
        input = ColourSpace.convert(input, ColourSpace.RGB);
        DisplayUtilities.display(input, frame);

        //Group together pixels with the same class that are touching each other
        GreyscaleConnectedComponentLabeler labeler = new GreyscaleConnectedComponentLabeler();
        List<ConnectedComponent> components = labeler.findComponents(input.flatten());

        //Draw an image with components numbered
        int i = 0;
        for (ConnectedComponent comp : components) {
            if (comp.calculateArea() < 50)
                continue;
            input.drawText("Point:" + (i++), comp.calculateCentroidPixel(), HersheyFont.TIMES_MEDIUM, 10);
        }

        DisplayUtilities.display(input, frame);


        // -- EXERCISE 3.1.1. The PixelProcessor
        // clone instantiated at the top of code

        clone = ColourSpace.convert(clone, ColourSpace.CIE_Lab);
        clone.processInplace(new PixelProcessor<Float[]>() {
            @Override
            public Float[] processPixel(Float[] pixel) {
                float[] newPixel = new float[pixel.length];
                //convert from Float[] to float[]
                for(int i = 0; i < pixel.length; i++){
                    newPixel[i] = pixel[i];
                }

                //return an index of the class that the pixel belongs to
                int centroid = assigner.assign(newPixel);

                //assingn new float array for the specific centroid
                float[] centroidPixel = centroids[centroid];

                for(int i =0; i < centroidPixel.length; i++){
                    pixel[i] = centroidPixel[i];
                }
                return pixel;
            }
        });
        clone = ColourSpace.convert(clone, ColourSpace.RGB);
        DisplayUtilities.display(clone, frame);

        /**
         * Pros and Cons of using PixelProcessor
         * Cons:
         *  - Converting Float[] to float[] slows down the process
         *  Pros:
         *  - Reusable code
         *
         */

        // -- EXERCISE 3.1.2. A real segmentation algorithm
        //create semgneter
        FelzenszwalbHuttenlocherSegmenter<MBFImage> segmenter = new FelzenszwalbHuttenlocherSegmenter();

        //Segment the segmentationClone MBFImage cloned from original input
        List<ConnectedComponent> ccs = segmenter.segment(segmentationClone);

        //Render segments onto an image
        SegmentationUtilities.renderSegments(segmentationClone, ccs);

        DisplayUtilities.display(segmentationClone, "Exercise 3.1.2");

        /**
         * In comparison to the basic segmentation algorithm, FelzenszwalbHuttenlocherSegmenter is considerably slower
         * and produces different results with every run. It seems that it is quite memory demanding which would not scale well with
         * image size.
         */




    }
}
