package nl.tue.ddss.bcf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;

import org.xml.sax.SAXException;

import javax.xml.parsers.*;

public class TempGeometry {
	ArrayList<String> stringList = new ArrayList<String>();
	ArrayList<Float> floatList = new ArrayList<Float>();
	ArrayList<Float> xList = new ArrayList<Float>();
	ArrayList<Float> yList = new ArrayList<Float>();
	ArrayList<Float> zList = new ArrayList<Float>();
	private BoundingBox boundingBox;
	private BoundingBox globalBB;
	double cameraViewPointX;
	double cameraViewPointY;
	double cameraViewPointZ;
	double cameraUpVectorX;
	double cameraUpVectorY;
	double cameraUpVectorZ;
	double cameraDirectionX;
	double cameraDirectionY;
	double cameraDirectionZ;
	
	
	public void setGlobalBB(){
		
	}

	void cleanUp(List<String> guids) throws SAXException,
			ParserConfigurationException {
		try {
			Matrix4d matrix = new Matrix4d();
			matrix.setIdentity();
			File tempFile = File.createTempFile("Model_View_Checker", null);
			boundingBox = ColladaBB.getBoundingBox(tempFile, matrix, guids);
			System.out.println(guids.size());
			System.out.println(guids.get(0));

			Point3d max = boundingBox.getMax();
			Point3d min = boundingBox.getMin();
			
			if (max==null||min==null){
				globalBB=ColladaGlobalBB.getGlobalBB(tempFile, matrix);
				max=globalBB.getMax();
				min=globalBB.getMin();
			}

			double centerX = (max.getX() + min.getX()) / 2;
			double centerY = (max.getY() + min.getY()) / 2;
			double centerZ = (max.getZ() + min.getZ()) / 2;
			double lengthX = max.getX() - min.getX();
			double lengthY = max.getY() - min.getY();
			double lengthZ = max.getZ() - min.getZ();
			
			double lengthMax=lengthX;
			if (lengthY>lengthMax) lengthMax=lengthY;
			if (lengthZ>lengthMax) lengthMax=lengthZ;

			cameraViewPointX = centerX
					+ ((2 * lengthMax) * Math.cos(Math.PI / 4));
			cameraViewPointY = centerY
					+ ((2 * lengthMax) * Math.cos(Math.PI / 4));
			cameraViewPointZ = centerZ + 0.5 * lengthMax;

			cameraDirectionX = centerX - cameraViewPointX;
			cameraDirectionY = centerY - cameraViewPointY;
			cameraDirectionZ = centerZ - cameraViewPointZ;

			cameraUpVectorX = cameraDirectionX;
			cameraUpVectorY = cameraDirectionY;
			cameraUpVectorZ = ((Math.pow(cameraDirectionX, 2) + Math.pow(
					cameraDirectionY, 2)) / -cameraDirectionZ);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}