package uk.ac.soton.ecs.ae2u16.ch7;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.processing.convolution.FGaussianConvolve;
import org.openimaj.image.processing.convolution.FourierConvolve;
import org.openimaj.image.processing.convolution.Gaussian2D;
import org.openimaj.image.processing.edges.CannyEdgeDetector;
import org.openimaj.image.processing.edges.SUSANEdgeDetector;
import org.openimaj.image.typography.hershey.HersheyFont;
import org.openimaj.video.Video;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;
import org.openimaj.video.capture.VideoCapture;
import org.openimaj.video.capture.VideoCaptureException;
import org.openimaj.video.xuggle.XuggleVideo;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * OpenIMAJ Hello world!
 *
 */
public class Ch7 {
    public static void main( String[] args ) throws MalformedURLException, VideoCaptureException {
    	//Create a video which holds coloured frames
        Video<MBFImage> video;

        //Use Xuggle library to load a video from a file
        video = new XuggleVideo(new URL("http://static.openimaj.org/media/tutorial/keyboardcat.flv"));

        //Video capture can also be used if computer has camera
        //video = new VideoCapture(320, 240);

        //Display video
        VideoDisplay<MBFImage> display = VideoDisplay.createVideoDisplay(video);

        //Process the frames of the video by iterating through each frame
        for(MBFImage mbfImage : video){
            DisplayUtilities.displayName(mbfImage.process(new CannyEdgeDetector()), "videoFrames");
        }

        //Tie processing to image display automatically by using an event driven technique
        display = VideoDisplay.createVideoDisplay(video);
        display.addVideoListener(
                new VideoDisplayListener<MBFImage>() {
                    public void beforeUpdate(MBFImage frame) {
                        frame.processInplace(new CannyEdgeDetector());
                    }

                    public void afterUpdate(VideoDisplay<MBFImage> display) {
                    }
                });
        //Benefit of the above approach is that functionality such as looping, pausing and stopping the video is given to you for free

        // -- EXERCISE 7.1.1. Applying different types of image processing to the video
        display = VideoDisplay.createVideoDisplay(video);
        display.addVideoListener(
                new VideoDisplayListener<MBFImage>() {
                    @Override
                    public void afterUpdate(VideoDisplay<MBFImage> videoDisplay) {

                    }

                    @Override
                    public void beforeUpdate(MBFImage frame) {
                        //Works similar to Canny Edge Detector
                        //frame.processInplace(new SUSANEdgeDetector());

                        //Applies Gaussian2D Fourier Convolution
                        frame.processInplace(new FourierConvolve(new Gaussian2D(3, 5f).createKernelImage(8, 9f)));
                    }
                }
        );
    }
}
