package uk.ac.soton.ecs.ae2u16.ch4;

import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.pixel.statistics.HistogramModel;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.math.statistics.distribution.MultidimensionalHistogram;

import javax.swing.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenIMAJ Hello world!
 *
 */
public class Ch4 {
    public static void main( String[] args ) throws IOException {
    	//Numerical representations of pixels are known as feature vectors representing features.

        JFrame frame = DisplayUtilities.createNamedWindow("frame", "Chapter 4", true);

        //Load 3 images, then generate and store the histograms
        //Added more images for more interesting findings during exercises
        URL[] imageURLs = new URL[] {
                new URL( "http://openimaj.org/tutorial/figs/hist1.jpg" ),
                new URL( "http://openimaj.org/tutorial/figs/hist2.jpg" ),
                new URL( "http://openimaj.org/tutorial/figs/hist3.jpg" ),
                new URL("https://images.unsplash.com/photo-1605973246991-c7e2a18882ad?ixlib=rb-1.2.1&ixid=MXwxMjA3fDB8MHxlZGl0b3JpYWwtZmVlZHwzfHx8ZW58MHx8fA%3D%3D&auto=format&fit=crop&w=500&q=60"),
                new URL("https://images.unsplash.com/photo-1605999182823-874e3b1d7a68?ixlib=rb-1.2.1&ixid=MXwxMjA3fDB8MHxlZGl0b3JpYWwtZmVlZHwxNHx8fGVufDB8fHw%3D&auto=format&fit=crop&w=500&q=60"),
                new URL("https://images.unsplash.com/photo-1605962519155-761f0e97f459?ixlib=rb-1.2.1&ixid=MXwxMjA3fDB8MHxlZGl0b3JpYWwtZmVlZHwyMnx8fGVufDB8fHw%3D&auto=format&fit=crop&w=500&q=60"),
                new URL("https://images.unsplash.com/photo-1497262693247-aa258f96c4f5?ixlib=rb-1.2.1&ixid=MXwxMjA3fDB8MHxzZWFyY2h8MTN8fGdyZWVjZXxlbnwwfHwwfA%3D%3D&auto=format&fit=crop&w=500&q=60"),
                new URL("https://images.unsplash.com/photo-1533104816931-20fa691ff6ca?ixlib=rb-1.2.1&ixid=MXwxMjA3fDB8MHxzZWFyY2h8Mnx8Z3JlZWNlfGVufDB8fDB8&auto=format&fit=crop&w=500&q=60")
        };

        List<MultidimensionalHistogram> histograms = new ArrayList<MultidimensionalHistogram>();
        HistogramModel model = new HistogramModel(4, 4, 4);

        for( URL u : imageURLs ) {
            model.estimateModel(ImageUtilities.readMBF(u));
            histograms.add( model.histogram.clone() );
        }

        //Storing the comparison using the Euclidean distance between two histograms ~ .35..
        double distanceScore = histograms.get(0).compare( histograms.get(1), DoubleFVComparison.EUCLIDEAN );

        //Comparing all the histograms with each other
        for( int i = 0; i < histograms.size(); i++ ) {
            for( int j = i; j < histograms.size(); j++ ) {
                double distance = histograms.get(i).compare( histograms.get(j), DoubleFVComparison.EUCLIDEAN );
                System.out.println(distance);
            }
        }

        // -- EXERCISE 4.1.1. Finding and displaying similar images

        //setting initial distance as 1 signifying no similarity
        double min = 1;
        //index pointers for two similar images
        int index1 = 0;
        int index2 = 0;
        //loop from the above example, but this time it does not compare with itself
        for( int i = 0; i < histograms.size(); i++ ) {
            for( int j = i + 1; j < histograms.size(); j++ ) {
                double distance = histograms.get(i).compare( histograms.get(j), DoubleFVComparison.EUCLIDEAN );
                //if distance is smaller than before, update pointers
                if(distance < min){
                    min = distance;
                    index1 = j;
                    index2 = i;
                }
            }
        }

        //create MBFImage array to display the two images
        MBFImage[] similarImages = new MBFImage[2];
        similarImages[0] = ImageUtilities.readMBF(imageURLs[index1]);
        similarImages[1] = ImageUtilities.readMBF(imageURLs[index2]);

        DisplayUtilities.displayLinked("Distance: " + min, 2, similarImages);


        // -- EXERCISE 4.1.2. Exploring comparison measures
        /**
         * Intersection seems to have the opposite outcome. Compared to the Euclidean distance,
         * intersection outputs a high distance when images are similar and low distance when
         * the similarity is low, hence the code below needed adjusting to achieve accurate results.
         */

        //creating similarity score and pointer variables
        double similarity = 0;
        int image1 = 0;
        int image2 = 0;

        //loop through the histograms and compare them using Intersection
        for(int i = 0; i < histograms.size(); i++){
            for(int j = i+1; j < histograms.size(); j++){
                double distance = histograms.get(i).compare( histograms.get(j), DoubleFVComparison.INTERSECTION );
                //if distance is bigger than the current similarity score, update
                if(distance > similarity){
                    similarity = distance;
                    image1 = i;
                    image2 = j;
                }
            }
        }

        //Create array and display most similar images
        MBFImage[] intersectionSimilarImgs = new MBFImage[2];
        intersectionSimilarImgs[0] = ImageUtilities.readMBF(imageURLs[image1]);
        intersectionSimilarImgs[1] = ImageUtilities.readMBF(imageURLs[image2]);

        DisplayUtilities.displayLinked("Similarity: " + similarity, 2, similarImages);



    }
}
