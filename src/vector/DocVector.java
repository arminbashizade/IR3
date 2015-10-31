package vector;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class DocVector
{
	public ArrayList<Integer> word;
	public ArrayList<Float> weight;
	
	public DocVector()
	{
	}
	
	public DocVector(ArrayList<Integer> word, ArrayList<Float> weight)
	{
		this.word = new ArrayList<Integer>(word);
		this.weight = new ArrayList<Float>(weight);
	}

	public DocVector(TrainDocVectorCalculator vectors, int docID)
	{
		word = new ArrayList<Integer>();
		weight = new ArrayList<Float>();
	}
	
	public DocVector(DocVectorCalculator vectors, int docID) throws IOException
	{
		word = new ArrayList<Integer>();
		weight = new ArrayList<Float>();
		long start = vectors.docPositionInVectorFile.get(docID);
		long end = vectors.docPositionInVectorFile.get(docID+1);
		String filePath = vectors.path;
		RandomAccessFile vectorsFile = new RandomAccessFile(filePath, "r");
		vectorsFile.seek(start);
		while(vectorsFile.getFilePointer() < end)
		{
			word.add(vectorsFile.readInt());
			weight.add(vectorsFile.readFloat());
		}
		vectorsFile.close();
	}
	
	public void setVector(ArrayList<Integer> word, ArrayList<Float> weight)
	{
		this.word = new ArrayList<Integer>(word);
		this.weight = new ArrayList<Float>(weight);
	}
}
