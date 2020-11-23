package uk.ac.soton.ecs.ae2u16;

import org.apache.commons.vfs2.FileSystemException;
import org.openimaj.data.dataset.MapBackedDataset;
import org.openimaj.data.dataset.VFSGroupDataset;
import org.openimaj.data.dataset.VFSListDataset;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.dataset.BingImageDataset;
import org.openimaj.image.dataset.FlickrImageDataset;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.util.api.auth.DefaultTokenFactory;
import org.openimaj.util.api.auth.common.BingAPIToken;
import org.openimaj.util.api.auth.common.FlickrAPIToken;
import org.scribe.builder.api.GoogleApi;

import java.util.ArrayList;
import java.util.Map;

/**
 * OpenIMAJ Hello world!
 *
 */
public class Ch6 {
    public static void main( String[] args ) throws Exception {
        /**
         * There are two types of datasets in OpenIMAJ: ListDatasets and GroupedDatasets
         * GroupedDataset is an extension of Java Map interface.
         */

        //Create a simple list dataset from a directory of images
        VFSListDataset<MBFImage> images = new VFSListDataset<MBFImage>("/Users/edelmans/Documents/university/Year 3/COMP3204-CompVis/Coursework1/image_dir", ImageUtilities.MBFIMAGE_READER);

        //Since ListDataset extends normal Java List, standard List operations work
        System.out.println(images.size());

        //Get random item from the dataset
        //DisplayUtilities.display(images.getRandomInstance(), "A random image from the dataset");

        //Display all images in a window
        //DisplayUtilities.display("My images", images);

        //Create dataset from zip file which is hosted on a web-server
        VFSListDataset<FImage> faces =
                new VFSListDataset<FImage>("zip:http://datasets.openimaj.org/att_faces.zip", ImageUtilities.FIMAGE_READER);
        //DisplayUtilities.display("ATT faces", faces);

        //Since ListDataset looses associations between images, GroupDataset can be used instead
        VFSGroupDataset<FImage> groupedFaces =
                new VFSGroupDataset<FImage>( "zip:http://datasets.openimaj.org/att_faces.zip", ImageUtilities.FIMAGE_READER);

        //Iterate through keys and display all the images from each individual in a window
        for (final Map.Entry<String, VFSListDataset<FImage>> entry : groupedFaces.entrySet()) {
            //DisplayUtilities.display(entry.getKey(), entry.getValue());
        }

        //Dynamically create a dataset of images from the web using FlickrImageDataset.
        FlickrAPIToken flickrToken = DefaultTokenFactory.get(FlickrAPIToken.class);
        FlickrImageDataset<MBFImage> landscapes =
                FlickrImageDataset.create(ImageUtilities.MBFIMAGE_READER, flickrToken, "landscape", 7);
        DisplayUtilities.display("Landscapes", landscapes);

        // -- EXERCISE 6.1.1. Exploring Grouped Datasets
        /**
         * Using provided faces dataset, display an image that shows a randomly selected photo of each person
         */
        //ArrayList for storing random faces
        ArrayList<FImage> randomFaces = new ArrayList<>();
        //iterate through all GroupDataset entries and pick one face that is added to randomFaces
        for(final Map.Entry<String, VFSListDataset<FImage>> entry : groupedFaces.entrySet()){
            randomFaces.add(entry.getValue().getRandomInstance());
        }

        //DisplayUtilities.display("Random faces", randomFaces);

        // -- EXERCISE 6.1.2. Find out more about VFS datasets
        /**
         * Explore documentation of the Commons VFS to see what kinds of sources are supported for building datasets
         * According to the documentation VFS supports a wide variety of sources like:
         * 1. FTP
         * 2. HTTP / HTTPS
         * 3. Jar
         * 4. Tar
         * 5. Zip
         * 6. GZIP
         */

        // -- EXERCISE 6.1.3. Try the BingImageDataset dataset
        BingAPIToken bingAPIToken = DefaultTokenFactory.get(BingAPIToken.class);
        //below two lines used to re-initiate API key request, since it failed on the first try
        DefaultTokenFactory.delete(BingAPIToken.class);
        DefaultTokenFactory.get(BingAPIToken.class);
        //Build dataset
        BingImageDataset<MBFImage> choppers =
                BingImageDataset.create(ImageUtilities.MBFIMAGE_READER, bingAPIToken, "Harley Davidson", 20);
        //Display dataset
        DisplayUtilities.display("Harley Davidson Motorcycles", choppers);

        // -- EXERCISE 6.1.4. Using MapBackedDataset
        //MapBackedDataset provides a concrete implementation of a GroupedDataset
        //Create an arraylist with celebrity names
        ArrayList<String> famousPeople = new ArrayList<>();
        famousPeople.add("Will Smith");
        famousPeople.add("Dwayne Johnson");
        famousPeople.add("Elon Musk");
        famousPeople.add("Tom Brady");

        //Create datasets for each name in the people arraylist
        ArrayList<BingImageDataset<MBFImage>> datasets = new ArrayList<>();
        for(String s : famousPeople){
            datasets.add(BingImageDataset.create(ImageUtilities.MBFIMAGE_READER, bingAPIToken, s, 5));
        }

        //Create a mapBackedDataset from the previous datasets
        MapBackedDataset<String, BingImageDataset<MBFImage>, MBFImage> celebrities = MapBackedDataset.of(datasets);


    }
}
