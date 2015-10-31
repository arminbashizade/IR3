package read;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

@SuppressWarnings("unused")
public class DocWordIndexNoFile
{
//	int currentDoc;
//	long currentFilePosition;
//	ArrayList<ArrayList<IDFrequency>> list;
//	
//	int size;
//	
//	public DocWordIndexNoFile(String fileName, int mode) throws IOException
//	{
//		list
//		currentFilePosition = 0;
//		size = 0;
//		currentDoc = -1;
//	}
//	
//	public void add(int doc, int wordID) throws IOException
//	{		
//		if(doc != currentDoc)
//		{
//			currentDoc = doc;
//			words.add(-1);
//			frequencies.add(-1);
//			currentFilePosition += 8;
//			
//			size++;
//		}
//		
//		if(!isDupilcate(wordID)) // updates the frequency if it is duplicate
//		{
//			words.add(wordID);
//			outputBuffer.putInt(wordID);
//			frequencies.add(1);
//			outputBuffer.putInt(1);
//			currentFilePosition += 8;
//			size++;
//		}
//	}
//	
//	private boolean isDupilcate(int wordID) throws IOException
//	{
//		long docPosInFile = docPositionInFile.get(currentDoc);
//		if(docPosInFile < docIndexFile.length())
//		{
//			//read from file
//			long filePos = docIndexFile.getFilePointer();
//			
//			long fileSize = docIndexFile.length();
//			long offset = docPositionInFile.get(currentDoc);
//			docIndexFile.seek(offset);
//			while(offset < fileSize)
//			{
//				int wordIDFromFile = docIndexFile.readInt();
//				int frequencyFromFile = docIndexFile.readInt();
//				offset += 8;
//				if(wordIDFromFile == wordID)
//				{
//					docIndexFile.seek(offset-4);
//					docIndexFile.writeInt(frequencyFromFile+1);
//					return true;
//				}
//			}
//			
//			docIndexFile.seek(filePos);
//			
//			if(currentDoc == docPositionInFile.size()-1)
//			{
//				int i = 0;
//				while(i < words.size() && words.get(i) != -1)
//				{
//					if(words.get(i) == wordID)
//					{
//						int f = frequencies.get(i);
//						int tmpPos = outputBuffer.position();
//						outputBuffer.position(i*8+4);
//						outputBuffer.putInt(f+1);
//						frequencies.set(i, f+1);
//						outputBuffer.position(tmpPos);
//						return true;
//					}
//					i++;
//				}
//			}
//		}
//		else
//		{
//			//read from ArrayList
//			int indexInArrayList = size-1;
//			while(words.get(indexInArrayList) != -1)
//				indexInArrayList--;
//
//			indexInArrayList++; // beginning of the word list
//			
//			for(int i = indexInArrayList; i < size;i++)
//			{
//				if(words.get(i) == wordID)
//				{
//					int f = frequencies.get(i);
//					int tmpPos = outputBuffer.position();
//					outputBuffer.position(i*8+4);
//					outputBuffer.putInt(f+1);
//					frequencies.set(i, f+1);
//					outputBuffer.position(tmpPos);
//					return true;
//				}
//			}
//		}
//		
//		return false;
//	}
//
//	public ArrayList<Integer> search(int docNum)
//	{
//		return null;
//	}
//	
//	public static void main(String[] args) throws IOException
//	{
//		DocWordIndexNoFile di = new DocWordIndexNoFile("docIndex", TEST_FILE);
//		di.add(1, 1);
//		di.add(1, 4);
//		di.add(1, 1);
//		di.add(1, 1);
//		di.add(1, 1);
//		di.add(1, 1);
//		di.add(2, 45);
//		di.add(3, 1);
//	}
}
class IDFrequency
{
	int ID;
	int frequency;
}

/*
package read;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class DocWordIndex
{
	public static final String TEST_OUTPUT_DIRECTORY = "C:/Users/maxi/Desktop/Dropbox/AUT/Information Storage and Retrieval/IR2/";
	public static final String MAIN_OUTPUT_DIRECTORY = "C:/Information Storage and Retrieval/";
	public static final int MAIN_FILE = 0;
	public static final int TEST_FILE = 1;
	private static final int BUFFER_SIZE = MyTokenizer.BUFF_SIZE;
	private static final int INTEGER_SIZE = 4;
	
	int currentDoc;
	long currentFilePosition;
	ArrayList<Integer> words;
	ArrayList<Integer> frequencies;
	ArrayList<Long> docPositionInFile;
	ArrayList<Integer> indexInBuffer;
	
	int size;
	String path;
	RandomAccessFile docIndexFile;
	private FileChannel writer;
	private ByteBuffer outputBuffer;
	
	public DocWordIndex(String fileName, int mode) throws IOException
	{
		indexInBuffer = new ArrayList<Integer>();
		words = new ArrayList<Integer>();
		frequencies = new ArrayList<Integer>();

		currentFilePosition = 0;
		size = 0;
		if(mode == TEST_FILE)
			path = TEST_OUTPUT_DIRECTORY + fileName;
		else
			path = MAIN_OUTPUT_DIRECTORY + fileName;
		
		new File(path).delete();
		
		docIndexFile = new RandomAccessFile(path, "rw");
		currentDoc = -1;
		
		docPositionInFile = new ArrayList<Long>();
		docPositionInFile.add((long) -1);
		
		writer = docIndexFile.getChannel();
		writer.position(0);
		outputBuffer = ByteBuffer.allocate(MyTokenizer.BUFF_SIZE);
		outputBuffer.clear();
	}
	
	public void add(int doc, int wordID) throws IOException
	{
		if(2*size*INTEGER_SIZE + 2*INTEGER_SIZE >= BUFFER_SIZE)
			writeToFile();
		
		if(doc != currentDoc)
		{
			currentDoc = doc;
			docPositionInFile.add(currentFilePosition);
			words.add(-1);
			frequencies.add(-1);
			outputBuffer.putInt(-1).putInt(-1);
			currentFilePosition += 8;
			
			size++;
		}
		
		if(!isDupilcate(wordID)) // updates the frequency if it is duplicate
		{
			words.add(wordID);
			outputBuffer.putInt(wordID);
			frequencies.add(1);
			outputBuffer.putInt(1);
			currentFilePosition += 8;
			size++;
		}
	}
	
	private boolean isDupilcate(int wordID) throws IOException
	{
		long docPosInFile = docPositionInFile.get(currentDoc);
		if(docPosInFile < docIndexFile.length())
		{
			//read from file
			long filePos = docIndexFile.getFilePointer();
			
			long fileSize = docIndexFile.length();
			long offset = docPositionInFile.get(currentDoc);
			docIndexFile.seek(offset);
			while(offset < fileSize)
			{
				int wordIDFromFile = docIndexFile.readInt();
				int frequencyFromFile = docIndexFile.readInt();
				offset += 8;
				if(wordIDFromFile == wordID)
				{
					docIndexFile.seek(offset-4);
					docIndexFile.writeInt(frequencyFromFile+1);
					return true;
				}
			}
			
			docIndexFile.seek(filePos);
			
			if(currentDoc == docPositionInFile.size()-1)
			{
				int i = 0;
				while(i < words.size() && words.get(i) != -1)
				{
					if(words.get(i) == wordID)
					{
						int f = frequencies.get(i);
						int tmpPos = outputBuffer.position();
						outputBuffer.position(i*8+4);
						outputBuffer.putInt(f+1);
						frequencies.set(i, f+1);
						outputBuffer.position(tmpPos);
						return true;
					}
					i++;
				}
			}
		}
		else
		{
			//read from ArrayList
			int indexInArrayList = size-1;
			while(words.get(indexInArrayList) != -1)
				indexInArrayList--;

			indexInArrayList++; // beginning of the word list
			
			for(int i = indexInArrayList; i < size;i++)
			{
				if(words.get(i) == wordID)
				{
					int f = frequencies.get(i);
					int tmpPos = outputBuffer.position();
					outputBuffer.position(i*8+4);
					outputBuffer.putInt(f+1);
					frequencies.set(i, f+1);
					outputBuffer.position(tmpPos);
					return true;
				}
			}
		}
		
		return false;
	}

	private void writeToFile() throws IOException
	{
		outputBuffer.flip();
		writer.write(outputBuffer);
		outputBuffer.clear();		
		words.clear();
		frequencies.clear();
		size = 0;
	}
	
	public void close() throws IOException
	{
		writeToFile();
		try {
			writer.close();
			docIndexFile.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public ArrayList<Integer> search(int docNum)
	{
		return null;
	}
	
	public void printFile() throws IOException
	{
		RandomAccessFile f = new RandomAccessFile(path, "r");
		
		int len = docPositionInFile.size();
		for(int i = 1; i < len; i++)
		{
			System.out.println("\ndoc "+i+":");
			f.seek(docPositionInFile.get(i)+8);
			while(true)
			{
				if(i < len-1)
				{
					if(f.getFilePointer() == docPositionInFile.get(i+1))
						break;
				}
				else if(f.getFilePointer() >= f.length())
					break;
				System.out.print("("+f.readInt()+"-"+f.readInt()+")\t");
			}
		}
		f.close();
	}
	
	public static void main(String[] args) throws IOException
	{
		DocWordIndex di = new DocWordIndex("docIndex", TEST_FILE);
		di.add(1, 1);
		di.add(1, 4);
		di.add(1, 1);
		di.add(1, 1);
		di.add(1, 1);
		di.add(1, 1);
		di.add(2, 45);
		di.add(3, 1);
		di.close();
		di.printFile();
	}
}

*/