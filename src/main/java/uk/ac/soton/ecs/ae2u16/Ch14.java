package uk.ac.soton.ecs.ae2u16;

import org.openimaj.data.dataset.GroupedDataset;
import org.openimaj.data.dataset.ListDataset;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.experiment.dataset.sampling.GroupSampler;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.annotation.evaluation.datasets.Caltech101;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.processing.resize.ResizeProcessor;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.time.Timer;
import org.openimaj.util.function.Operation;
import org.openimaj.util.parallel.Parallel;
import org.openimaj.util.parallel.partition.RangePartitioner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * OpenIMAJ Hello world!
 *
 */
public class Ch14 {
    public static void main( String[] args ) throws IOException {
        /**
         * Task of this chapter is to take advantage of multiple processors in java by creating and managing threads
         * or through the use of java.util.concurrent package
         *
         * OpenIMAJ includes Parallel class to allow efficient and effective creation of multi-threaded loops
         */

        //Write a parallel equivalent of a given for loop
        Parallel.forIndex(0, 10, 1, new Operation<Integer>() {
            public void perform(Integer i) {
                System.out.println(i);
            }
        });
        //Numbers are not printed in the correct order, meaning that when parallelizing a loop, the order is not deterministic

        //Build a program to compute the normalized average of the image in CalTech 101 dataset.
        //Load CalTech 101 dataset of images directly
        VFSGroupDataset<MBFImage> allImages = Caltech101.getImages(ImageUtilities.MBFIMAGE_READER);

        //Only use a subset of the first 8 groups
        GroupedDataset<String, ListDataset<MBFImage>, MBFImage> images = GroupSampler.sample(allImages, 8, false);


        List<MBFImage> output = new ArrayList<MBFImage>();
        ResizeProcessor resize = new ResizeProcessor(200);
        //Start timer
        Timer t1 = Timer.timer();
        //Loop through the images in group
        for (ListDataset<MBFImage> clzImages : images.values()) {
            MBFImage current = new MBFImage(200, 200, ColourSpace.RGB);

            for (MBFImage i : clzImages) {
                MBFImage tmp = new MBFImage(200, 200, ColourSpace.RGB);
                tmp.fill(RGBColour.WHITE);

                //Resize and normalize each image
                MBFImage small = i.process(resize).normalise();
                int x = (200 - small.getWidth()) / 2;
                int y = (200 - small.getHeight()) / 2;
                //Draw it on the center of a white image
                tmp.drawImage(small, x, y);

                //Add the result to an accumulator
                current.addInplace(tmp);
            }
            //Divide the accumulated image by the number of samples used to create it
            current.divideInplace((float) clzImages.size());
            output.add(current);
        }
        //Print time
        System.out.println("Time: " + t1.duration() + "ms");

        //Display results of the loop above
        DisplayUtilities.display("Images", output);
        /**
         * Can you tell what object is depicted by each average image?
         * Not really.. It is clear that second image on top row is a plane, followed by an image of a fish.
         * The second image of the second row seems to be a spider or an ant. Other images are too noisy/distorted.
         *
         * But I see how a lot of easily identifiable images could be too easy fro classification experiments.
         */

        //Parallelize the code through one of three methods: parallelizing the outer loop (1), parallelizing the inner one(2) or both (3)
        output = new ArrayList<MBFImage>();
        resize = new ResizeProcessor(200);
        //Start timer2
        t1 = Timer.timer();
        for (ListDataset<MBFImage> clzImages : images.values()) {
            //Current needed to be final for parallelizing
            final MBFImage current = new MBFImage(200, 200, ColourSpace.RGB);

            final ResizeProcessor finalResize = resize;
            //This tutorial focuses on the inner loop using Parallel.for method to rewrite the inner loop
            Parallel.forEach(clzImages, new Operation<MBFImage>() {
                public void perform(MBFImage i) {
                    final MBFImage tmp = new MBFImage(200, 200, ColourSpace.RGB);
                    tmp.fill(RGBColour.WHITE);

                    final MBFImage small = i.process(finalResize).normalise();
                    final int x = (200 - small.getWidth()) / 2;
                    final int y = (200 - small.getHeight()) / 2;
                    tmp.drawImage(small, x, y);

                    synchronized (current) {
                        current.addInplace(tmp);
                    }
                }
            });

            current.divideInplace((float) clzImages.size());
            output.add(current);
        }
        //Print time
        System.out.println("Time with parallelization: " + t1.duration() + "ms");

        /**
         * Improvements are noticeable right away
         * Time without parallelization: 14005ms
         * Time with parallelization: 4708ms
         */

        //Using parittioned variant for the for-each loop in the Parallel class and giving each thread a collection of images
        output = new ArrayList<MBFImage>();
        resize = new ResizeProcessor(200);
        //Start timer2
        t1 = Timer.timer();
        for (ListDataset<MBFImage> clzImages : images.values()) {
            //Current needed to be final for parallelizing
            final MBFImage current = new MBFImage(200, 200, ColourSpace.RGB);

            //This tutorial focuses on the inner loop using Parallel.for method to rewrite the inner loop
            final ResizeProcessor finalResize1 = resize;
            //RangePartitioner breaks the images into as many chunks as there are cores
            Parallel.forEachPartitioned(new RangePartitioner<MBFImage>(clzImages), new Operation<Iterator<MBFImage>>() {
                public void perform(Iterator<MBFImage> it) {
                    MBFImage tmpAccum = new MBFImage(200, 200, 3);
                    MBFImage tmp = new MBFImage(200, 200, ColourSpace.RGB);

                    while (it.hasNext()) {
                        final MBFImage i = it.next();
                        tmp.fill(RGBColour.WHITE);

                        final MBFImage small = i.process(finalResize1).normalise();
                        final int x = (200 - small.getWidth()) / 2;
                        final int y = (200 - small.getHeight()) / 2;
                        tmp.drawImage(small, x, y);
                        tmpAccum.addInplace(tmp);
                    }
                    synchronized (current) {
                        current.addInplace(tmpAccum);
                    }
                }
            });

            current.divideInplace((float) clzImages.size());
            output.add(current);
        }
        //Print time
        System.out.println("Time with parallelization and partitioned variant of for-each loop: " + t1.duration() + "ms");

        /**
         * Time: 13630ms
         * Time with parallelization: 4568ms
         * Time with parallelization and partitioned variant of for-each loop: 4321ms
         */

        /**
         * -- EXERCISE 14.1.1. Parallelize the outer loop
         * Modify the code to parallelise the outer loop and record the improvement
         * What are the pros and cons of doing it?
         *
         * Time with parallelizing the outer loop: 7271ms
         * It seems that parallelizing the outer loop does not yield better results than inner loop parallelization
         *
         * Pros:
         * Less communication between threads than inner loop parallelization
         * Cons:
         * Higher amount of memory usage
         */
        output = new ArrayList<MBFImage>();
        resize = new ResizeProcessor(200);
        //Start timer
        t1 = Timer.timer();
        //Loop through the images in group
        final ResizeProcessor finalResize2 = resize;
        final List<MBFImage> finalOutput = output;
        Parallel.forEach (images.values(), new Operation<ListDataset<MBFImage>>() {
            @Override
            public void perform(ListDataset<MBFImage> clzImages){
                MBFImage current = new MBFImage(200, 200, ColourSpace.RGB);

                for (MBFImage i : clzImages) {
                    MBFImage tmp = new MBFImage(200, 200, ColourSpace.RGB);
                    tmp.fill(RGBColour.WHITE);

                    //Resize and normalize each image
                    MBFImage small = i.process(finalResize2).normalise();
                    int x = (200 - small.getWidth()) / 2;
                    int y = (200 - small.getHeight()) / 2;
                    //Draw it on the center of a white image
                    tmp.drawImage(small, x, y);

                    //Add the result to an accumulator
                    synchronized (current) {
                        current.addInplace(tmp);
                    }
                }
                //Divide the accumulated image by the number of samples used to create it
                current.divideInplace((float) clzImages.size());
                finalOutput.add(current);
            }
        });
        //Print time
        System.out.println("Time with parallelizing the outer loop: " + t1.duration() + "ms");




    }
}
