#
# Make script for building camera calibration application.
# To compile stereo camera calibrator OpenCV and Java glue needed.
# also ImageJ is added to use as imaging utility lib.
#
# Fredrik Hederstierna 2013
#
# javacv-0.6-cppjars.zip
#  ---> ffmpeg-2.0.1-linux-x86_64.jar
#  ---> opencv-2.4.6.1-linux-x86_64.jar
# javacv-0.6-bin.zip
#  ---> javacpp.jar
#  ---> javacv.jar
#  ---> javacv-linux-x86_64.jar
# ij147.zip
#  ---> ij.jar
#
# Links:
# https://code.google.com/p/javacv/
# http://rsb.info.nih.gov/ij/
#

SC = scalac
SR = scala

BUILD = ../build

OPENCV_DIR =	/home/fredrik/github/opencv

OPENCV_LIB =	
OPENCV_LIB +=	$(OPENCV_DIR)/javacv.jar
OPENCV_LIB +=	$(OPENCV_DIR)/javacv-linux-x86_64.jar
OPENCV_LIB +=	$(OPENCV_DIR)/javacpp.jar
OPENCV_LIB +=	$(OPENCV_DIR)/opencv-2.4.6.1-linux-x86_64.jar
OPENCV_LIB +=	$(OPENCV_DIR)/ffmpeg-2.0.1-linux-x86_64.jar
OPENCV_LIB +=	$(OPENCV_DIR)/ij.jar

# convert to colon separated list
SPACE := $(eval) $(eval)
PATHS := $(subst $(SPACE),:,$(OPENCV_LIB))

all:
#	@echo "CLASSPATH: " $(PATHS)
	$(SC) -d $(BUILD) -cp .$(PATHS) *.scala
run:
	$(SR) -classpath $(BUILD)$(PATHS) com.stereoscan.camcalibrator.Main
