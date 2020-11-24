package uk.ac.soton.ecs.ae2u16;

import org.apache.commons.vfs2.FileSystemException;
import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.experiment.dataset.split.GroupedRandomSplitter;
import org.openimaj.experiment.dataset.util.DatasetAdaptors;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.model.EigenImages;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.typography.hershey.HersheyFont;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenIMAJ Hello world!
 *
 */
public class Ch13 {
    public static void main( String[] args ) throws FileSystemException {
        //Implementing earliest successful face recognition algorithms called "Eigenfaces"
        /**
         * Idea: face images are "projected" into a low dimensional space where they are compared efficiently.
         * Fundamentally this projection is a form of feature extraction
         * But unlike the previous extractors, Eigenfaces have to "learn" the feature extractor from the image data
         *
         * The lower dimensional space is learned through a process called Principle Component Analysis
         */

        //Load the dataset of approximately aligned faces
        VFSGroupDataset<FImage> dataset =
                new VFSGroupDataset<FImage>("zip:http://datasets.openimaj.org/att_faces.zip", ImageUtilities.FIMAGE_READER);

        /**
         * -- EXERCISE 13.1.2. Explore the effect of training set size
         * Reduce the number of training images whilst keeping number of testing images fixed at 5
         * Record observations
         *
         * With smaller training sets the accuracy is as follows:
         * 4: 0.905
         * 3: 0.875
         * 2: 0.79
         * 1: 0.655
         */
        //Split dataset into two halves for training and testing
        //nTraining can be reduced to test Exercise 13.1.2.
        int nTraining = 4;
        int nTesting = 5;
        GroupedRandomSplitter<String, FImage> splits =
                new GroupedRandomSplitter<String, FImage>(dataset, nTraining, 0, nTesting);
        GroupedDataset<String, ListDataset<FImage>, FImage> training = splits.getTrainingDataset();
        GroupedDataset<String, ListDataset<FImage>, FImage> testing = splits.getTestDataset();

        //Use training images to learn the PCA basis
        List<FImage> basisImages = DatasetAdaptors.asList(training);
        int nEigenvectors = 100;
        EigenImages eigen = new EigenImages(nEigenvectors);
        eigen.train(basisImages);

        //Any face image can be decomposed as weighted summation of the basis vector
        //Draw the first 12 basis vectors (otherwise known as EigenFace)
        List<FImage> eigenFaces = new ArrayList<FImage>();
        for (int i = 0; i < 12; i++) {
            eigenFaces.add(eigen.visualisePC(i));
        }
        DisplayUtilities.display("EigenFaces", eigenFaces);

        //Build a database of features from training images using Map of Strings and an array of features
        Map<String, DoubleFV[]> features = new HashMap<String, DoubleFV[]>();
        for (final String person : training.getGroups()) {
            final DoubleFV[] fvs = new DoubleFV[nTraining];

            for (int i = 0; i < nTraining; i++) {
                final FImage face = training.get(person).get(i);
                fvs[i] = eigen.extractFeature(face);
            }
            features.put(person, fvs);
        }

        //Extract the feature from an image, find database feature with smallest distance and return identifier of the person
        //Loop through test set and estimate which person they belong to.
        double correct = 0, incorrect = 0;
        for (String truePerson : testing.getGroups()) {
            for (FImage face : testing.get(truePerson)) {
                DoubleFV testFeature = eigen.extractFeature(face);

                String bestPerson = null;
                double minDistance = Double.MAX_VALUE;
                for (final String person : features.keySet()) {
                    for (final DoubleFV fv : features.get(person)) {
                        double distance = fv.compare(testFeature, DoubleFVComparison.EUCLIDEAN);

                        /**
                         * -- EXERCISE 13.1.3. Apply a threshold
                         * If distance is greater than a threshold, return unknown result.
                         * Good threshold value will result in the same accuracy as without the threshold
                         * because rather than guessing wrong, it returns null for bestPerson
                         *
                         * Through trial and error it seems that 15 yields good accuracy whilst not being close to the
                         * longest observed distance.
                         */
                        if(distance > 15){
                            System.out.print("| Unkown face |");
                            continue;
                        }

                        if (distance < minDistance) {
                            minDistance = distance;
                            bestPerson = person;
                        }
                    }
                }

                System.out.println("Actual: " + truePerson + "\tguess: " + bestPerson);

                if (truePerson.equals(bestPerson))
                    correct++;
                else
                    incorrect++;
                }
            }

            double curAccuracy = correct / (correct + incorrect);
            System.out.println("Accuracy: " + curAccuracy);
            // Accuracy when nTraining is 5: 0.94


        /**
         * -- EXERCISE 13.1.1. Reconstructing faces
         * It is possible to reconstruct an estimate of the original image from the feature.
         *  - Build a PCA basis as described above
         *  - Extract the feature of a randomly selected face from the test-set
         *  - Use EigenImages#reconstruct() to convert the feature back into an image
         *  - Normalize the image (FImage#normalise())
         *  - Display result
         *
         */

        //Create arrayList to store original image and reconstructed image from feature
        ArrayList<FImage> reconArr = new ArrayList<>();

        //Randomly select a face from test set
        FImage randomFace = testing.getRandomInstance();
        reconArr.add(randomFace);

        //Extract a feature of a randomly selected face
        DoubleFV randomFeature = eigen.extractFeature(randomFace);

        //Convert feature back to an image
        randomFace = eigen.reconstruct(randomFeature);

        //Normalize the image
        randomFace = randomFace.normalise();
        reconArr.add(randomFace);

        //Display result
        DisplayUtilities.display("Reconstructed from feature", reconArr);




    }
}
