package read;

import java.io.IOException;
import java.util.HashMap;

import dictionary.Train;

public class TrainDocWordIndex
{
	private static final int LARGE_WAGON_SIZE = 1000*1000;
	private static final int SMALL_WAGON_SIZE = 50*1000;
	
	Train startInList = null;
	Train docTerm = null;
	Train docTermFrequencies = null;
	HashMap<Integer, Integer> currentDocWordIndexHash = null;
	Integer indexInTrain = null;
	int currentDoc = 0;
	int lastIndex = 0;
	
	int numberOfDocs = 0;
	
	public TrainDocWordIndex()
	{
		startInList = new Train(SMALL_WAGON_SIZE, Train.Type.INTEGER);
		docTerm = new Train(LARGE_WAGON_SIZE, Train.Type.INTEGER);
		docTermFrequencies = new Train(LARGE_WAGON_SIZE, Train.Type.INTEGER);
		currentDocWordIndexHash = new HashMap<Integer, Integer>();
		currentDoc = 0;
		numberOfDocs = 0;
		lastIndex = 0;
	}

	public void add(int doc, int wordID) throws IOException
	{
		if(doc != currentDoc)
		{
			currentDoc = doc;
			startInList.setInt(currentDoc, lastIndex);
			currentDocWordIndexHash.clear();
			numberOfDocs++;
		}
		
		indexInTrain = currentDocWordIndexHash.get(wordID);
		if(indexInTrain == null)
		{
			docTerm.setInt(lastIndex, wordID);
			docTermFrequencies.setInt(lastIndex, 1);
			currentDocWordIndexHash.put(wordID, lastIndex);
			lastIndex++;
		}
		else
			docTermFrequencies.setInt(indexInTrain, docTermFrequencies.getInt(indexInTrain)+1);
	}
	
	public void finish()
	{
		startInList.setInt(currentDoc+1, lastIndex);
	}
	
	int size;
	int offset;
	int i;
	public int[] getWords(int docID)
	{
		offset = startInList.getInt(docID);
		size = startInList.getInt(docID+1)-startInList.getInt(docID);
		int[] result = new int[size];
		
		for(i = 0; i < size; i++)
			result[i] = docTerm.getInt(offset+i);
		
		return result;
	}
	
	public int[] getFrequencies(int docID)
	{
		offset = startInList.getInt(docID);
		size = startInList.getInt(docID+1)-startInList.getInt(docID);
		int[] result = new int[size];
		
		for(i = 0; i < size; i++)
			result[i] = docTermFrequencies.getInt(offset+i);
		
		return result;
	}
	
	public int getSize(int docID)
	{
		return startInList.getInt(docID+1)-startInList.getInt(docID);
	}

	public int getNumberOfDocs()
	{
		return numberOfDocs;
	}
	
	public void clear()
	{
		startInList = new Train(1, Train.Type.INTEGER);
		docTerm = new Train(1, Train.Type.INTEGER);
		docTermFrequencies = new Train(1, Train.Type.INTEGER);
		currentDocWordIndexHash = new HashMap<Integer, Integer>();		
	}
	
	public static void main(String[] args) throws IOException
	{
		TrainDocWordIndex di = new TrainDocWordIndex();
		di.add(1, 1);
		di.add(1, 2);
		di.add(1, 3);
		di.add(1, 1);
		
		di.add(2, 1);
		di.add(2, 1);
		di.add(2, 1);
		di.add(2, 1);

		
		di.finish();

//		System.out.println(di.getTF(2, 2));

		System.out.println(di.getNumberOfDocs());
		
		for(int i = 1; i <= di.getNumberOfDocs(); i++)
		{
			for(int j = 0; j < di.getSize(i); j++)
			{
				System.out.println(di.getWords(i)[j]+"\t"+di.getFrequencies(i)[j]);
			}
		}

	}
}
