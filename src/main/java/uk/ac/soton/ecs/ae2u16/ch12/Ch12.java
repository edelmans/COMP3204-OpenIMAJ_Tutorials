package uk.ac.soton.ecs.ae2u16.ch12;

import de.bwaldvogel.liblinear.SolverType;
import org.openimaj.data.DataSource;
import org.openimaj.data.dataset.Dataset;
import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.VFSListDataset;
import org.openimaj.experiment.dataset.sampling.GroupSampler;
import org.openimaj.experiment.dataset.sampling.GroupedUniformRandomisedSampler;
import org.openimaj.experiment.dataset.split.GroupedRandomSplitter;
import org.openimaj.experiment.evaluation.classification.ClassificationEvaluator;
import org.openimaj.experiment.evaluation.classification.ClassificationResult;
import org.openimaj.experiment.evaluation.classification.analysers.confusionmatrix.CMAnalyser;
import org.openimaj.experiment.evaluation.classification.analysers.confusionmatrix.CMResult;
import org.openimaj.feature.DiskCachingFeatureExtractor;
import org.openimaj.feature.DoubleFV;
import org.openimaj.feature.FeatureExtractor;
import org.openimaj.feature.SparseIntFV;
import org.openimaj.feature.local.data.LocalFeatureListDataSource;
import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.annotation.evaluation.datasets.Caltech101;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.feature.dense.gradient.dsift.ByteDSIFTKeypoint;
import org.openimaj.image.feature.dense.gradient.dsift.DenseSIFT;
import org.openimaj.image.feature.dense.gradient.dsift.PyramidDenseSIFT;
import org.openimaj.image.feature.local.aggregate.BagOfVisualWords;
import org.openimaj.image.feature.local.aggregate.BlockSpatialAggregator;
import org.openimaj.image.feature.local.aggregate.PyramidSpatialAggregator;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.io.IOUtils;
import org.openimaj.ml.annotation.linear.LiblinearAnnotator;
import org.openimaj.ml.clustering.ByteCentroidsResult;
import org.openimaj.ml.clustering.assignment.HardAssigner;
import org.openimaj.ml.clustering.kmeans.ByteKMeans;
import org.openimaj.ml.kernel.HomogeneousKernelMap;
import org.openimaj.util.pair.IntFloatPair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenIMAJ Hello world!
 *
 */
public class Ch12 {
    public static void main( String[] args ) throws IOException {
    	//Going through steps to build and evaluate a near state-of-the-art image clssifier

        //Automatically download and set up Caltech 101 dataset
        GroupedDataset<String, VFSListDataset<Caltech101.Record<FImage>>, Caltech101.Record<FImage>> allData =
                Caltech101.getData(ImageUtilities.FIMAGE_READER);
        //allData object is a GroupedDataset with String keys and the values are VFSListDatasets

        //Record class holds metadata about each Caltech 101 image.

        //Get a subset of the classes in the dataset to minimise the run time of the program using GroupSamples class
        GroupedDataset<String, ListDataset<Caltech101.Record<FImage>>, Caltech101.Record<FImage>> data =
                GroupSampler.sample(allData, 5, false);

        //Create two sets of images: a training set to learn the classifier and a testing set
        //Choose a number of training instances for each class of images
        // -- EXERCISE 12.1.3. The whole dataset (Pt 1/4): Code below has been modified to use allData instead of data
        GroupedRandomSplitter<String, Caltech101.Record<FImage>> splits =
                new GroupedRandomSplitter<String, Caltech101.Record<FImage>>(allData, 15, 0, 15);

        //Consider extracting suitable image features using Pyramid Histogram of Words (PHOW)
        //First construct a Dense SIFT extractor and PyramidDenseSift

        // -- EXERCISE 12.1.3. Pt(2/4): Reducing DenseSift step size to 3, and pdsift sizes to [4,6,8,10]
        DenseSIFT dsift = new DenseSIFT(3, 7);
        PyramidDenseSIFT<FImage> pdsift = new PyramidDenseSIFT<FImage>(dsift, 6f, 4,6,8,10);

        // -- EXERCISE 12.1.2. Feature caching (See further code for continuation of 12.1.2.
        HardAssigner<byte[], float[], IntFloatPair> assigner;
        //Try loading an assigner
        try{
            assigner = IOUtils.readFromFile(new File("hardAssigner.txt"));
        }catch (FileNotFoundException e){
            //Assigner not found, so create a new assigner and save it
            assigner = trainQuantiser(GroupedUniformRandomisedSampler.sample(splits.getTrainingDataset(), 30), pdsift);
            IOUtils.writeToFile(assigner, new File("hardAssigner.txt"));
        }

//        //Perform K-Means clustering on a random sample of 30 images across all groups
//        HardAssigner<byte[], float[], IntFloatPair> assigner =
//                trainQuantiser(GroupedUniformRandomisedSampler.sample(splits.getTrainingDataset(), 30), pdsift);

        //Create an instance of PHOWExtractor
        FeatureExtractor<DoubleFV, Caltech101.Record<FImage>> extractor = new PHOWExtractor(pdsift, assigner);

        // -- EXERCISE 12.1.1. Apply a Homogeneous Kernel Map
        HomogeneousKernelMap newKMap = new HomogeneousKernelMap(HomogeneousKernelMap.KernelType.Chi2, HomogeneousKernelMap.WindowType.Rectangular);
        //extractor = newKMap.createWrappedExtractor(extractor);
        /**
         * Running classifier without Homogeneous Kernel Map
         * Time elapsed: 68 seconds
         * Accuracy: 0.693
         * Error Rate: 0.307
         *
         * Running classifier with Homogeneous Kernel Map
         * Time elapsed: 73 seconds
         * Accuracy: 0.787
         * Error Rate: 0.213
         *
         * Compared to the initial extractor, having a Homogeneous Kernel Map increases accuracy,
         * reduces error rate, but that comes at a cost of runtime.
         */

        //Keeping a record of the time
        long start = System.currentTimeMillis();
        System.out.println("Time counter starts now");

        // -- EXERCISE 12.1.2. Feature caching Continued (See lines 80-89 for part 1)
        DiskCachingFeatureExtractor featureCache = new DiskCachingFeatureExtractor(new File("Extractor"), extractor);

        //Construct and train  linear classifier using LiblinearAnnotator class
        LiblinearAnnotator<Caltech101.Record<FImage>, String> ann = new LiblinearAnnotator<Caltech101.Record<FImage>, String>(
                extractor, LiblinearAnnotator.Mode.MULTICLASS, SolverType.L2R_L2LOSS_SVC, 1.0, 0.00001);

        // -- EXERCISE 12.1.2. Feature caching final part (See lines 80-89 and 121 for previous parts)
        ann = new LiblinearAnnotator<Caltech101.Record<FImage>, String>(
                featureCache, LiblinearAnnotator.Mode.MULTICLASS, SolverType.L2R_L2LOSS_SVC, 1.0, 0.00001);
        ann.train(splits.getTrainingDataset());

        //Perform an automated evaluation of classifier's accuracy
        ClassificationEvaluator<CMResult<String>, String, Caltech101.Record<FImage>> eval =
                new ClassificationEvaluator<CMResult<String>, String, Caltech101.Record<FImage>>(
                        ann, splits.getTestDataset(), new CMAnalyser<Caltech101.Record<FImage>, String>(CMAnalyser.Strategy.SINGLE));

        Map<Caltech101.Record<FImage>, ClassificationResult<String>> guesses = eval.evaluate();
        CMResult<String> result = eval.analyse(guesses);

        //Calculate elapsed time
        long timeElapsed = (System.currentTimeMillis() - start) / 1000;
        System.out.println("Result: " + result + " | Time elapsed: " + timeElapsed);

        /** 
         * -- EXERCISE 12.1.3. Results
         * The adjusted code had the following performance
         * Accuracy: 0.295
         * Error Rate: 0.705 
         * Time Elapsed: 3 hours 11 minutes
         *
         * It seems that it performed the worst, which could mean two things:
         * 1. There was an error in my implementation
         * 2. The code does not perform well on large datasets
         *
         */





    }

    //Perform K-Means clustering on a sample of SIFT features to build a HardAssigner
    static HardAssigner<byte[], float[], IntFloatPair> trainQuantiser(
            Dataset<Caltech101.Record<FImage>> sample, PyramidDenseSIFT<FImage> pdsift) throws IOException {
        List<LocalFeatureList<ByteDSIFTKeypoint>> allkeys = new ArrayList<LocalFeatureList<ByteDSIFTKeypoint>>();

        for (Caltech101.Record<FImage> rec : sample) {
            FImage img = rec.getImage();

            pdsift.analyseImage(img);
            allkeys.add(pdsift.getByteKeypoints(0.005f));
        }

        if (allkeys.size() > 10000)
            allkeys = allkeys.subList(0, 10000);

        // -- EXERCISE 12.1.3. The whole dataset (Pt 3/4): Code below has been modified to have 600 visual words
        ByteKMeans km = ByteKMeans.createKDTreeEnsemble(600);
        DataSource<byte[]> datasource = new LocalFeatureListDataSource<ByteDSIFTKeypoint, byte[]>(allkeys);
        ByteCentroidsResult result = km.cluster(datasource);

        return result.defaultHardAssigner();
    }

    //Create a FeatureExtractor that will be used to train classifier
    static class PHOWExtractor implements FeatureExtractor<DoubleFV, Caltech101.Record<FImage>> {
        PyramidDenseSIFT<FImage> pdsift;
        HardAssigner<byte[], float[], IntFloatPair> assigner;

        public PHOWExtractor(PyramidDenseSIFT<FImage> pdsift, HardAssigner<byte[], float[], IntFloatPair> assigner)
        {
            this.pdsift = pdsift;
            this.assigner = assigner;
        }

        public DoubleFV extractFeature(Caltech101.Record<FImage> object) {
            FImage image = object.getImage();
            pdsift.analyseImage(image);

            BagOfVisualWords<byte[]> bovw = new BagOfVisualWords<byte[]>(assigner);

            // -- EXERCISE 12.1.3. (Pt 4/4): Using PyramidSPatialAggregator with [2,4] blocks instead of BlockSpatialAggregator
            PyramidSpatialAggregator<byte[], SparseIntFV> spatial = new PyramidSpatialAggregator<byte[], SparseIntFV>(bovw, 2, 4);
            //BlockSpatialAggregator<byte[], SparseIntFV> spatial = new BlockSpatialAggregator<byte[], SparseIntFV>(
            //        bovw, 2, 2);

            return spatial.aggregate(pdsift.getByteKeypoints(0.015f), image.getBounds()).normaliseFV();
        }
    }

}
