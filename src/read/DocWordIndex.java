package read;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;

public class DocWordIndex
{
	private static final int BUFFER_SIZE = MyTokenizer.BUFF_SIZE;
	private static final String TEST_OUTPUT_DIRECTORY = "C:/Users/maxi/Desktop/Dropbox/AUT/Information Storage and Retrieval/IR2/";
	private static final String MAIN_OUTPUT_DIRECTORY = "C:/Information Storage and Retrieval/";
	public static final int MAIN_FILE = 0;
	public static final int TEST_FILE = 1;
	private static final int INTEGER_SIZE = 4;
	
	int currentDoc;
	long currentFilePosition;
	HashMap<Integer, Integer> wordIndexHash;
	Integer indexInArray;
	int lastIndex;
	ArrayList<ArrayList<Integer>> docTerm;
	ArrayList<ArrayList<Integer>> docTermFrequencies;
	ArrayList<Integer> words;
	ArrayList<Integer> frequencies;
	ArrayList<Long> docPositionInFile;

	int i;
	int size;
	String path;
	String fileName;
	RandomAccessFile docIndexFile;
	RandomAccessFile inputFile;
	FileChannel writer;
	FileChannel reader;
	ByteBuffer writeBuffer;
	ByteBuffer readBuffer;
	int totalDoc;
	
	public DocWordIndex(String fileName, int mode) throws IOException
	{
		docTerm = new ArrayList<ArrayList<Integer>>();
		docTermFrequencies = new ArrayList<ArrayList<Integer>>();
		docTerm.add(new ArrayList<Integer>());
		docTermFrequencies.add(new ArrayList<Integer>());
		lastIndex = 0;
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
		inputFile = new RandomAccessFile(path, "r");
		currentDoc = -1;
		
		docPositionInFile = new ArrayList<Long>();
		docPositionInFile.add((long) -1); //first doc index is 1
		wordIndexHash = new HashMap<Integer, Integer>();

		writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		writer = docIndexFile.getChannel();
		reader = inputFile.getChannel();
	}
	
	public DocWordIndex(String fileName) throws FileNotFoundException
	{
		docTerm = new ArrayList<ArrayList<Integer>>();
		docTermFrequencies = new ArrayList<ArrayList<Integer>>();
		docTerm.add(new ArrayList<Integer>());
		docTermFrequencies.add(new ArrayList<Integer>());
		writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		totalDoc = 0;
		lastIndex = 0;
		words = new ArrayList<Integer>();
		frequencies = new ArrayList<Integer>();

		currentFilePosition = 0;
		size = 0;

		this.fileName = fileName;
		
		currentDoc = -1;
		
		docPositionInFile = new ArrayList<Long>();
		docPositionInFile.add((long) -1); //first doc index is 1
		wordIndexHash = new HashMap<Integer, Integer>();
	}

	public void reset()
	{
		writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		writeBuffer.clear();
		
		totalDoc = 0;
		lastIndex = 0;
		docTerm = new ArrayList<ArrayList<Integer>>();
		docTermFrequencies = new ArrayList<ArrayList<Integer>>();
		docTerm.add(new ArrayList<Integer>());
		docTermFrequencies.add(new ArrayList<Integer>());
		words = new ArrayList<Integer>();
		frequencies = new ArrayList<Integer>();

		currentFilePosition = 0;
		size = 0;
		currentDoc = -1;
		
		docPositionInFile = new ArrayList<Long>();
		docPositionInFile.add((long) -1); //first doc index is 1
		wordIndexHash = new HashMap<Integer, Integer>();
	}
	
	public void setPath(String path) throws FileNotFoundException
	{
		this.path = path + "\\" + fileName;
		new File(this.path).delete();
		docIndexFile = new RandomAccessFile(this.path, "rw");
		inputFile = new RandomAccessFile(this.path, "r");
		writer = docIndexFile.getChannel();
		reader = inputFile.getChannel();
	}
	
	public void add(int doc, int wordID) throws IOException
	{
		if(doc != currentDoc)
		{
			currentDoc = doc;
			if(lastIndex != 0)
				writeToFile();
			lastIndex = 0;
			words.clear();
			frequencies.clear();
			wordIndexHash.clear();
		}
		
		indexInArray = wordIndexHash.get(wordID);
		if(indexInArray == null)
		{
			words.add(wordID);
			frequencies.add(1);
			wordIndexHash.put(wordID, lastIndex);
			lastIndex++;
		}
		else
			frequencies.set(indexInArray, frequencies.get(indexInArray)+1);
		
	}
	
	private void writeToFile() throws IOException
	{
		totalDoc++;
		size = words.size();
		
		docPositionInFile.add(docIndexFile.getFilePointer()+writeBuffer.position());
		if(writeBuffer.remaining() < 2*size*INTEGER_SIZE) // write to file
		{
			writeBuffer.flip();
			writer.write(writeBuffer);
			writeBuffer.clear();

			if(writeBuffer.capacity() < 2*size*INTEGER_SIZE)
			{
				int n = 1;
				while(n*BUFFER_SIZE < size*INTEGER_SIZE*2)
					n++;
				writeBuffer = ByteBuffer.allocate(n*BUFFER_SIZE);
				writeBuffer.clear();
			}
		}

//		docTerm.add(new ArrayList<Integer>(words));
//		docTermFrequencies.add(new ArrayList<Integer>(frequencies));
		
		for(i = 0; i < size; i++)
			writeBuffer.putInt(words.get(i)).putInt(frequencies.get(i));

		writeBuffer.putInt(-1).putInt(-1);	
		
//		docPositionInFile.add(docIndexFile.getFilePointer());
//		for(i = 0; i < size; i++)
//		{
//			docIndexFile.writeInt(words.get(i));
//			docIndexFile.writeInt(frequencies.get(i));
//		}
//		docIndexFile.writeInt(-1);
//		docIndexFile.writeInt(-1);
	}
	
	public void close() throws IOException
	{
//		writeToFile();
		totalDoc++;

		size = words.size();
		
		docPositionInFile.add(docIndexFile.getFilePointer()+writeBuffer.position());
		if(writeBuffer.remaining() < 2*size*INTEGER_SIZE) // write to file
		{
			writeBuffer.flip();
			writer.write(writeBuffer);
			writeBuffer.clear();

			if(writeBuffer.capacity() < 2*size*INTEGER_SIZE)
			{
				int n = 1;
				while(n*BUFFER_SIZE < size*INTEGER_SIZE*2)
					n++;
				writeBuffer = ByteBuffer.allocate(n*BUFFER_SIZE);
				writeBuffer.clear();
			}
		}
		
//		docTerm.add(new ArrayList<Integer>(words));
//		docTermFrequencies.add(new ArrayList<Integer>(frequencies));
		
		for(i = 0; i < size; i++)
			writeBuffer.putInt(words.get(i)).putInt(frequencies.get(i));

		writeBuffer.putInt(-1).putInt(-1);	

		writeBuffer.flip();
		writer.write(writeBuffer);
		writeBuffer.clear();
		try {
			writer.close();
			docIndexFile.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<Integer> search(int docNum)
	{
		return null;
	}


	public ArrayList<Integer> getNextDocWords(int docID) throws IOException
	{
		ArrayList<Integer> result = new ArrayList<Integer>();
		
		return result;
	}
	
	public ArrayList<Integer> getNextDocFrequencies(int docID) throws IOException
	{
		ArrayList<Integer> result = new ArrayList<Integer>();
		
		return result;
	}
	
	public ArrayList<Integer> getWords(int docID) throws IOException
	{
//		return docTerm.get(docID);
		
		RandomAccessFile tfFile = new RandomAccessFile(path, "r");
		ArrayList<Integer> result;
		result = new ArrayList<Integer>();
		tfFile.seek(docPositionInFile.get(docID));
		wordInFile = 0;
		while(wordInFile != -1)
		{
			wordInFile = tfFile.readInt();
			tfInFile = tfFile.readInt();
			
			if(wordInFile != -1)
			{
				result.add(wordInFile);
			}
		}
		
		tfFile.close();
		return result;
	}
	
	public ArrayList<Integer> getFrequencies(int docID) throws IOException
	{
//		return docTermFrequencies.get(docID);
		
		ArrayList<Integer> result;
		result = new ArrayList<Integer>();
		RandomAccessFile tfFile = new RandomAccessFile(path, "r");
		tfFile.seek(docPositionInFile.get(docID));
		wordInFile = 0;
		while(wordInFile != -1)
		{
			wordInFile = tfFile.readInt();
			tfInFile = tfFile.readInt();
			
			if(tfInFile != -1)
			{
				result.add(tfInFile);
			}
		}
		
		tfFile.close();
		return result;
	}
	
	int wordInFile, tfInFile;
	public int getTF(int term, int doc) throws IOException
	{
		RandomAccessFile tfFile = new RandomAccessFile(path, "r");
		tfFile.seek(docPositionInFile.get(doc));
		wordInFile = 0;
		while(wordInFile != -1)
		{
			wordInFile = tfFile.readInt();
			tfInFile = tfFile.readInt();
			if(wordInFile == term)
			{
				tfFile.close();
				return tfInFile;
			}
		}
		
		tfFile.close();
		return 0;
	}
	
	public int getTotalDoc()
	{
		return totalDoc;
	}
	
	public void printFile() throws IOException
	{
		RandomAccessFile f = new RandomAccessFile(path, "r");
		
		int len = docPositionInFile.size();
		for(int i = 1; i < len; i++)
		{
			System.out.println("\ndoc "+i+":");
			f.seek(docPositionInFile.get(i));
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
		System.out.println();
		f.close();
	}
	
	public void clear() throws IOException
	{
		if(inputFile != null)
			inputFile.close();
		if(docIndexFile != null)
			docIndexFile.close();
		if(path != null)
			new File(path).delete();
	}
	
	public static void main(String[] args) throws IOException
	{
		DocWordIndex di = new DocWordIndex("docIndex", TEST_FILE);
		di.add(1, 1);
		di.add(1, 2);
		di.add(1, 3);
		di.add(1, 1);
		
		di.add(2, 1);
		di.add(2, 1);
		di.add(2, 1);
		di.add(2, 1);

		di.close();
//		System.out.println(di.getTF(2, 2));
		di.printFile();

		System.out.println(di.getTotalDoc());
		
		for(int i = 1; i <= di.getTotalDoc(); i++)
		{
			for(int j = 0; j < di.getWords(i).size(); j++)
			{
				System.out.println(di.getWords(i).get(j)+"\t"+di.getFrequencies(i).get(j));
			}
		}
	}
}
