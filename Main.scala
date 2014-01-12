
package com.stereoscan.camcalibrator;

import java.io.{FileNotFoundException, IOException, File}

import java.awt._
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage

import javax.swing.JFrame
import javax.swing.WindowConstants

import com.googlecode.javacpp.Pointer

import com.googlecode.javacv.CanvasFrame
import com.googlecode.javacv.FrameGrabber
import com.googlecode.javacv.OpenCVFrameGrabber

//--import static com.googlecode.javacv.cpp.opencv_core.cvFlip
//import com.googlecode.javacv.cpp.opencv_imgproc._
//import com.googlecode.javacv.cpp.opencv_features2d.{DMatch, KeyPoint}

import com.googlecode.javacv.cpp.opencv_calib3d._
import com.googlecode.javacv.cpp.opencv_core.IplImage
import com.googlecode.javacv.cpp.opencv_core.{CvMat, CvSize, cvGetSize, CvPoint2D32f}
import com.googlecode.javacv.cpp.opencv_highgui._

import scala.collection.mutable.ArrayBuffer

object Main extends App {

    // ------- chess board setup
    final val CHESS_PATTERN_COLS: Int = 9
    final val CHESS_PATTERN_ROWS: Int = 6
    final val CAMERA_LEFT_PORT  : Int = 1
    final val CAMERA_RIGHT_PORT : Int = 2


    /** Load an image and show in a CanvasFrame.
      *
      * If image cannot be loaded the application will exit with code 1.
      *
      * @param file image file
      * @param flags Flags specifying the color type of a loaded image:
      *              <ul>
      *              <li> `>0` Return a 3-channel color image</li>
      *              <li> `=0` Return a gray scale image</li>
      *              <li> `<0` Return the loaded image as is. Note that in the current implementation
      *              the alpha channel, if any, is stripped from the output image. For example, a 4-channel
      *              RGBA image is loaded as RGB if the `flags` is greater than 0.</li>
      *              </ul>
      *              Default is gray scale.
      * @return Loaded image
      */
    def loadAndShowOrExit(file: File, flags: Int = CV_LOAD_IMAGE_GRAYSCALE): IplImage = {
        try {
            val image = loadOrExit(file, flags)
            //--            show(image, file.getName)
            image
        }
        catch {
            case ex: IOException => {
                println("Couldn't load image: " + file.getAbsolutePath)
                sys.exit(1)
            }
        }
    }


    /** Load an image, if image cannot be loaded the application will exit with code 1.
      *
      * @param file image file
      * @param flags Flags specifying the color type of a loaded image:
      *              <ul>
      *              <li> `>0` Return a 3-channel color image</li>
      *              <li> `=0` Return a gray scale image</li>
      *              <li> `<0` Return the loaded image as is. Note that in the current implementation
      *              the alpha channel, if any, is stripped from the output image. For example, a 4-channel
      *              RGBA image is loaded as RGB if the `flags` is greater than 0.</li>
      *              </ul>
      *              Default is gray scale.
      * @throws FileNotFoundException when file does not exist
      * @throws IOException when image cannot be read
      * @return Loaded image
      */
    def loadOrExit(file: File, flags: Int = CV_LOAD_IMAGE_GRAYSCALE): IplImage = {
        // Verify file
        if (!file.exists()) {
            throw new FileNotFoundException("Image file does not exist: " + file.getAbsolutePath)
        }
        // Read input image
        val image = cvLoadImage(file.getAbsolutePath, flags)
        if (image == null) {
            throw new IOException("Couldn't load image: " + file.getAbsolutePath)
        }
        image
    }




    /** Show image in a window. Closing the window will exit the application. */
    def show(image: IplImage, title: String) {
        val canvas = new CanvasFrame(title, 1)
        canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        canvas.showImage(image)
    }


    /** Show image in a window. Closing the window will exit the application. */
    def show(mat: CvMat, title: String) {
        val canvas = new CanvasFrame(title, 1)
        canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        canvas.showImage(mat.asIplImage())
    }


    /** Show image in a window. Closing the window will exit the application. */
    def show(image: Image, title: String) {
        val canvas = new CanvasFrame(title, 1)
        canvas.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        canvas.showImage(image)
    }

    /** Open grabber ant take some images */
    def setupCamera(port: Int, frames: Int): OpenCVFrameGrabber = {
      //---------Code start Main
      val grabber = new OpenCVFrameGrabber(port)
      grabber.start()
      var imgGrab = grabber.grab()
      //--------tune exposure
      for (i <- 0 until 20) {
        imgGrab = grabber.grab()
      }
      grabber
    }


    //--------------------
    def calibrateCamera(file: File) {

    // Create calibrator object
    val cameraCalibrator = new CameraCalibrator()

      // file list
    val fileListL = new ArrayBuffer[File]   
    fileListL += file


    // Add the corners from the chessboard
    val boardSize = new CvSize(CHESS_PATTERN_COLS, CHESS_PATTERN_ROWS)
    cameraCalibrator.addChessboardPoints(fileListL, boardSize)


    // Load image for that will be undistorted
    val imageL = loadAndShowOrExit(fileListL(0))

    // Calibrate camera
    cameraCalibrator.calibrate(cvGetSize(imageL))

    // Undistort
    val undistortedL = cameraCalibrator.remap(imageL)

    // Display camera matrix
    val mL = cameraCalibrator.cameraMatrix
    println("Camera intrinsic: " + mL.rows + "x" + mL.cols)
    for (i <- 0 until 3) {
        for (j <- 0 until 3) {
          println("%7.2f  ".format(mL.get(i, j)))
        }
        println("")
    }

    //--------show corners
    // Load image for that will be matched with checker board
    val imageL2 = loadAndShowOrExit(fileListL(0))

    // Find chessboard corners
    val patternSize = new CvSize(CHESS_PATTERN_COLS, CHESS_PATTERN_ROWS)
    val corners = new CvPoint2D32f(patternSize.width * patternSize.height)
    val cornerCount = Array(1)
    val flags = CV_CALIB_CB_ADAPTIVE_THRESH | CV_CALIB_CB_NORMALIZE_IMAGE
    val patternFound = cvFindChessboardCorners(imageL2, patternSize, corners, cornerCount, flags)

    println("pattern found: " + patternFound)
    // Draw the corners on image
    cvDrawChessboardCorners(imageL2, patternSize, corners, cornerCount(0), patternFound)
    show(imageL2, "ChessboardCorners: " + file.getName)


    //-----------show undistored image
    //--cvFlip(img, img, 1);// l-r = 90_degrees_steps_anti_clockwise
    show(undistortedL, "Undistorted: " + file.getName)
    }

    //---------- main code


    //--------take images

    println("Taking image Left...")
    val grabberL = setupCamera(CAMERA_LEFT_PORT, 20)
    val imgL = grabberL.grab()
    if (imgL == null) {
      println("Take Left Fail, Abort")
      System.exit(1)
    }
    val sL = "camSnapshotLeft.jpg"
    val fL = new File(sL)
    val fileListL = new ArrayBuffer[File]   
    fileListL += fL
    cvSaveImage(sL, imgL)
    println("Snapshot, stop.")
    grabberL.stop()
    println("Snapshot, flush.")
      //grabberL.flush()
    println("Snapshot, done.")


    println("Taking image Right...")
    val grabberR = setupCamera(CAMERA_RIGHT_PORT, 20)
    val imgR = grabberR.grab()
    if (imgR == null) {
      println("Take Right Fail, Abort")
      System.exit(1)
    }
    val sR = "camSnapshotRight.jpg"
    val fR = new File(sR)
    cvSaveImage(sR, imgR)
    println("Snapshot, stop.")
    grabberR.stop()
    println("Snapshot, flush.")
      //grabberR.flush()
    println("Snapshot, done.")


    //------------ calibrate
    calibrateCamera(fL)
    calibrateCamera(fR)
}
