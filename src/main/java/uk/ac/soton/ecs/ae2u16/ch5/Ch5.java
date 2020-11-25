package uk.ac.soton.ecs.ae2u16.ch5;

import com.sun.xml.bind.v2.TODO;
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
import org.openimaj.math.geometry.transforms.AffineTransformModel;
import org.openimaj.math.geometry.transforms.HomographyModel;
import org.openimaj.math.geometry.transforms.HomographyRefinement;
import org.openimaj.math.geometry.transforms.estimation.RobustAffineTransformEstimator;
import org.openimaj.math.geometry.transforms.estimation.RobustHomographyEstimator;
import org.openimaj.math.geometry.transforms.residuals.AlgebraicResidual2d;
import org.openimaj.math.model.fit.LMedS;
import org.openimaj.math.model.fit.RANSAC;

import javax.swing.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * OpenIMAJ Hello world!
 *
 */
public class Ch5 {
    //Creating an array to store the results of different matchers
    static ArrayList<MBFImage> images = new ArrayList<>();

    public static void main( String[] args ) throws IOException {
    	//Looking at how to compare images to each other using local feature descriptor called SIFT
        //Creating a named window for better display
        JFrame frame = DisplayUtilities.createNamedWindow("frame", "Chapter 5", true);

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
        DisplayUtilities.display(basicMatches, frame);

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

        DisplayUtilities.display(consistentMatches, frame);

        //Draw a polygon around the estimated location of the query within the target
//        MBFImage target2 = target.clone();
//        target2.drawShape(
//        query.getBounds().transform(modelFitter.getModel().getTransform().inverse()), 3,RGBColour.BLUE);
//        DisplayUtilities.display(target2);

        // -- EXERCISE 5.1.1. Different matchers

        //Load 2 images
        MBFImage image1 = ImageUtilities.readMBF(new URL("http://static.openimaj.org/media/tutorial/query.jpg"));
        MBFImage image2 = ImageUtilities.readMBF(new URL("http://static.openimaj.org/media/tutorial/target.jpg"));

        //Create an array of matchers that will be experimented
        ArrayList<LocalFeatureMatcher<Keypoint>> matchers = new ArrayList<>();
        matchers.add(new BasicMatcher<Keypoint>(80));
        matchers.add(new BasicTwoWayMatcher<Keypoint>());
        matchers.add(new MultipleMatchesMatcher<Keypoint>(100, 50));
        matchers.add(new FastBasicKeypointMatcher<Keypoint>(80));

        //Test each matcher through "doMatching(matcher, img1, img2)" helper method and store result in MBFImage arraylist
        for(LocalFeatureMatcher<Keypoint> m : matchers){
            images.add(doMatching(m, image1, image2));
        }

        DisplayUtilities.display("5.1.1", images);
        /**
         * Based on the findings, BasicTwoWayMatcher performs a lot better than BasicMatcher.
         * which could be due to comparing the object both ways, rejecting one way matches.
         *
         * Whereas BasicMatcher just checks whether the two interest points have a sufficiently large distance.
         *
         * Other matchers were experimented with.. FastBasicKeypointMatcher outputted similar results
         * to BasicMatcher
         */


        // -- EXERCISE 5.1.2. Different models

        //Load 2 images
        target = ImageUtilities.readMBF(new URL("http://static.openimaj.org/media/tutorial/query.jpg"));
        query = ImageUtilities.readMBF(new URL("http://static.openimaj.org/media/tutorial/target.jpg"));

        queryKeypoints = engine.findFeatures(query.flatten());
        targetKeypoints = engine.findFeatures(target.flatten());

        RobustHomographyEstimator fitter = new RobustHomographyEstimator(50, 1500, new RANSAC.PercentageInliersStoppingCondition(0.5), HomographyRefinement.NONE);
        matcher = new ConsistentLocalFeatureMatcher2d<Keypoint>(
                new FastBasicKeypointMatcher<Keypoint>(8), fitter);
        matcher.setModelFeatures(queryKeypoints);
        matcher.findMatches(targetKeypoints);

        MBFImage homographyMatcher = MatchingUtilities.drawMatches(query, target, matcher.getMatches(), RGBColour.CYAN);

        target.drawShape(
        query.getBounds().transform(fitter.getModel().getTransform().inverse()), 3,RGBColour.BLUE);
        DisplayUtilities.display(target);
        DisplayUtilities.display(homographyMatcher, "5.1.2. Using Homography Model");

        //Works particularly well as it nw also matches the title of the paper

        //Trying out LMedS
        target = ImageUtilities.readMBF(new URL("http://static.openimaj.org/media/tutorial/query.jpg"));
        queryKeypoints = engine.findFeatures(query.flatten());
        targetKeypoints = engine.findFeatures(target.flatten());

        LMedS lMedSFitter = new LMedS(new AffineTransformModel(), new AlgebraicResidual2d(), true);
        matcher = new ConsistentLocalFeatureMatcher2d<Keypoint>(
                new FastBasicKeypointMatcher<Keypoint>(8), lMedSFitter);
        matcher.setModelFeatures(queryKeypoints);
        matcher.findMatches(targetKeypoints);

        MBFImage lMedSMatched = MatchingUtilities.drawMatches(query, target, matcher.getMatches(), RGBColour.CYAN);
        DisplayUtilities.display(homographyMatcher, "5.1.2. Using LMedS Model");

        /**
         * LMedS was a bit harder to implement, but yielded similar results to the Homography model
         */

    }

    // Helper function used for 5.1.1.
    public static MBFImage doMatching(LocalFeatureMatcher<Keypoint> featureMatcher, MBFImage img1, MBFImage img2){
        //Set up difference-of-Gaussian described with SIFT
        DoGSIFTEngine engine = new DoGSIFTEngine();
        //find features of both flattened images
        LocalFeatureList<Keypoint> queryKeypoints = engine.findFeatures(img1.flatten());
        LocalFeatureList<Keypoint> targetKeypoints = engine.findFeatures(img2.flatten());

        //Find matches between two images
        featureMatcher.setModelFeatures(queryKeypoints);
        featureMatcher.findMatches(targetKeypoints);

        //Display matched features with a line
        MBFImage basicMatches = MatchingUtilities.drawMatches(img1, img2, featureMatcher.getMatches(), RGBColour.RED);
        return basicMatches;
    }
}
