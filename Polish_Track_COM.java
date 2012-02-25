import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.*;
import ij.plugin.filter.*;

import ij.measure.*;
import ij.text.*;
import ij.io.*;
import java.text.DecimalFormat;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;


public class Polish_Track_COM implements PlugInFilter {
    ImagePlus imp;
    static int hw = 3; // half of window size in pixels 
    double baseIntensity = 1.0;  // as percentile

    public int setup(String arg, ImagePlus imp) {
	this.imp = imp;
	return DOES_8G + DOES_16 + DOES_32;
    }

    boolean withinCircle(double x, double y) {
	if ( (x-hw+1)*(x-hw+1)+(y-hw+1)*(y-hw+1)<=hw*hw ) 
	    return true; 
	else 
	    return false;
    }

    public Point2D.Double calcCentreOfMass(ImageProcessor ip, double bg) 
    {
	int width = ip.getWidth();
	int height = ip.getHeight();
	Point2D.Double centre = new Point2D.Double();

	float[] pixels = (float[])ip.getPixels();
	int count = 0;

	double sumx = 0, sumy = 0, sumg = 0;
	for (int i = 0; i<height; i++) {
	    for (int j = 0; j<width; j++) {
		if (withinCircle(j, i)) {
		    int l=i*width+j;
		    sumx += j*(pixels[l]-bg);
		    sumy += i*(pixels[l]-bg);
		    sumg += pixels[l]-bg;
		}
	    }
	}
	centre.setLocation(sumx/sumg, sumy/sumg);
	return centre;
    }

    private void fixResultsTable(ResultsTable rt, int tr, double cx, double cy) {
	if (tr >= 0 && tr < rt.getCounter()) {
	    rt.setValue("cx", tr, cx);
	    rt.setValue("cy", tr, cy);
	} else {
	    IJ.log("No corresponding rows in Results Tabe.");
	}
	return;
    }

    public void run(ImageProcessor ip) {

	int width = ip.getWidth();
	int height = ip.getHeight();
	Point2D.Double centre = new Point2D.Double();

	ImageStack stack = imp.getStack();
	int nSlices = stack.getSize();

	GenericDialog gd = new GenericDialog("Find Centre of Mass");
	gd.addNumericField("Half window size", hw, 0);
	gd.addNumericField("Background intensity in percentile", baseIntensity, 1);
	gd.showDialog();
	if (gd.wasCanceled()) return;
	hw = (int) gd.getNextNumber();
	baseIntensity = (double) gd.getNextNumber();

	ResultsTable rt = ResultsTable.getResultsTable();
	if (rt == null) {
	    IJ.error("Can't find Results Table.");
	    return;
	}

	int nResults = rt.getCounter();
	float[] fr = rt.getColumn(1);

	for (int n = 1 ; n <= nSlices; n++) {
	    ip = stack.getProcessor(n).convertToFloat();

	    float[] pixels = (float[]) ip.getPixels();
	    DescriptiveStatistics ds = new DescriptiveStatistics();
	    for (int i = 0; i < pixels.length; i++)
		ds.addValue((double)pixels[i]);
	    double bg = ds.getPercentile(baseIntensity);
	    double cx = -1;
	    double cy = -1;
	    for (int i = 0; i < nResults; i++) {
		if (n == (int)fr[i]) {
		    int X = (int)rt.getValueAsDouble(2, i);
		    int Y = (int)rt.getValueAsDouble(3, i);
		    if ( X>=hw && Y>=hw && X+hw<width && Y+hw<height ) {
			ip.setRoi(X-hw, Y-hw, 2*hw, 2*hw);
			centre = calcCentreOfMass(ip.crop(), bg);
			cx = rt.getValueAsDouble(2, i) - hw + centre.getX();
			cy = rt.getValueAsDouble(3, i) - hw + centre.getY();
		    } 
		    fixResultsTable(rt, i, cx, cy);
		}
	    }
	    rt.show("Results");
	}
    }
}
