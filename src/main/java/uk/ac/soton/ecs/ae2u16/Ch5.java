package uk.ac.soton.ecs.ae2u16;

import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.matcher.*;
import org.openimaj.feature.local.matcher.consistent.ConsistentLocalFeatureMatcher2d;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.feature.local.engine.DoGSIFTEngine;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.math.geometry.transforms.estimation.RobustAffineTransformEstimator;
import org.openimaj.math.model.fit.RANSAC;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * OpenIMAJ Hello world!
 *
 */
public class Ch5 {
    public static void main( String[] args ) throws IOException {
    	//Looking at how to compare images to each other using local feature descriptor called SIFT

        //Load 2 images
        MBFImage query = ImageUtilities.readMBF(new URL("http://static.openimaj.org/media/tutorial/query.jpg"));
        MBFImage target = ImageUtilities.readMBF(new URL("http://static.openimaj.org/media/tutorial/target.jpg"));

        //First, we use the difference-of-Gaussian described with a SIFT descriptor
        //The features are invariant to size changes, rotation and position
        DoGSIFTEngine engine = new DoGSIFTEngine();
        LocalFeatureList<Keypoint> queryKeypoints = engine.findFeatures(query.flatten());
        LocalFeatureList<Keypoint> targetKeypoints = engine.findFeatures(target.flatten());

        //We can use engine to extract Keypoint objects from images.
        //Take a given Keypoint in the query and find the keypoint closest in the target
        LocalFeatureMatcher<Keypoint> matcher = new BasicMatcher<Keypoint>(80);
        matcher.setModelFeatures(queryKeypoints);
        matcher.findMatches(targetKeypoints);

        //Draw the matches between two images using MatchingUtilities class
        MBFImage basicMatches = MatchingUtilities.drawMatches(query, target, matcher.getMatches(), RGBColour.RED);
        DisplayUtilities.display(basicMatches);

        //Filter matches based on a given geometric model
        //Set up RANSAC model fitter configured to find Affine Transforms
        RobustAffineTransformEstimator modelFitter = new RobustAffineTransformEstimator(50.0, 1500,
                new RANSAC.PercentageInliersStoppingCondition(0.5));
        matcher = new ConsistentLocalFeatureMatcher2d<Keypoint>(
                new FastBasicKeypointMatcher<Keypoint>(8), modelFitter);

        matcher.setModelFeatures(queryKeypoints);
        matcher.findMatches(targetKeypoints);

        MBFImage consistentMatches = MatchingUtilities.drawMatches(query, target, matcher.getMatches(),
                RGBColour.RED);

        DisplayUtilities.display(consistentMatches);

        //Draw a polygon around the estimated location of the query within the target
        target.drawShape(
                query.getBounds().transform(modelFitter.getModel().getTransform().inverse()), 3,RGBColour.BLUE);
        DisplayUtilities.display(target);

        // -- EXERCISE 5.1.1. Exploring comparison measures
        //Setup a different matcher and find matches in the target
        LocalFeatureMatcher<Keypoint> matcher2 = new BasicTwoWayMatcher<Keypoint>();
        matcher2.setModelFeatures(queryKeypoints);
        matcher2.findMatches(targetKeypoints);

        //Filter matches based on a given geometric model using previously created modelFitter



    }
}
