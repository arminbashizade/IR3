package dictionary;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import read.MyTokenizer;

public class TrainTST
{	
	private static final int WAGON_SIZE = 1000*1000;
	private static final int BUFFER_SIZE = MyTokenizer.BUFF_SIZE;
	private static final int CHARACTER_SIZE = 1;
	private static final int ID_SIZE = 4;
	private static final int LO_KID_INDEX_SIZE = 4;
	private static final int EQUAL_KID_INDEX_SIZE = 4;
	private static final int HI_KID_INDEX_SIZE = 4;
	private static final int FREQUENCY_SIZE = 4;
	private static final int BLOCK_SIZE = CHARACTER_SIZE+ID_SIZE+LO_KID_INDEX_SIZE+EQUAL_KID_INDEX_SIZE+HI_KID_INDEX_SIZE;
	
	Train character = new Train(WAGON_SIZE, Train.Type.BYTE);
	Train ID = new Train(WAGON_SIZE, Train.Type.INTEGER);
	Train frequency = new Train(WAGON_SIZE, Train.Type.INTEGER);
	Train loKidIndex = new Train(WAGON_SIZE, Train.Type.INTEGER);
	Train hiKidIndex = new Train(WAGON_SIZE, Train.Type.INTEGER);
	Train equalKidIndex = new Train(WAGON_SIZE, Train.Type.INTEGER);
	Train listEnds = new Train(600*1000, Train.Type.INTEGER);
	Train documentFrequnecies = new Train(600*1000, Train.Type.INTEGER);

	boolean isEmpty = true;
	public int numberOfNodes = 0;
	public int lastID = 1;
	int lastIndex = 1;
	int lastIndexOnLists = 1;
	private boolean docExistsInPosting;

	long frequenciesStartInFile;
	public String path;
	String fileName;
	RandomAccessFile dictionaryFile;
	FileChannel writer;
	ByteBuffer writeBuffer;
	
	public TrainTST()
	{
		writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		character = new Train(WAGON_SIZE, Train.Type.BYTE);
		ID = new Train(WAGON_SIZE, Train.Type.INTEGER);
		frequency = new Train(WAGON_SIZE, Train.Type.INTEGER);
		loKidIndex = new Train(WAGON_SIZE, Train.Type.INTEGER);
		hiKidIndex = new Train(WAGON_SIZE, Train.Type.INTEGER);
		equalKidIndex = new Train(WAGON_SIZE, Train.Type.INTEGER);
		listEnds = new Train(600*1000, Train.Type.INTEGER);
		documentFrequnecies = new Train(600*1000, Train.Type.INTEGER);

		isEmpty = true;
		numberOfNodes = 0;
		lastID = 1;
		lastIndex = 1;
		lastIndexOnLists = 1;

		for(int i = 0; i < WAGON_SIZE; i++)
		{
			character.setByte(i, (byte)0);
			ID.setInt(i, 0);
			frequency.setInt(i, 0);
			loKidIndex.setInt(i, 0);
			hiKidIndex.setInt(i, 0);
			equalKidIndex.setInt(i, 0);
		}
	}
	
	public int add(String s, int doc)
	{
		if(isEmpty)
		{
			isEmpty = false;
			numberOfNodes++;
		
			character.setByte(1, (byte) s.charAt(0));
			loKidIndex.setInt(1, 0);
			hiKidIndex.setInt(1, 0);
			equalKidIndex.setInt(1, 0);
			
			if(s.length() == 1)
			{
				ID.setInt(1, lastID);
				lastID++;
			}
			lastIndex++;
		}
		
		int i = 0;
		int stringSize = s.length();
		int index = 1;
		while(i < stringSize)
		{
			byte nodeChar = character.getByte(index);
			if(s.charAt(i) < nodeChar)
			{
//				if(n.loKid == null)
				if(loKidIndex.getInt(index) == 0)
				{
					numberOfNodes++;
					loKidIndex.setInt(index, lastIndex);
					character.setByte(lastIndex, (byte)s.charAt(i));
					lastIndex++;
				}
				else
					index = loKidIndex.getInt(index);
			}
			else if(s.charAt(i) > nodeChar)
			{
//				if(n.hiKid == null)
				if(hiKidIndex.getInt(index) == 0)
				{
					numberOfNodes++;
					hiKidIndex.setInt(index, lastIndex);
					character.setByte(lastIndex, (byte)s.charAt(i));
					lastIndex++;
				}
				else
					index = hiKidIndex.getInt(index);
			}
			else
			{
				if(i == stringSize - 1)
				{
					if(ID.getInt(index) == 0)
					{
						ID.setInt(index, lastID);
						listEnds.setInt(lastID, doc);
						frequency.setInt(index, frequency.getInt(index)+1);
						documentFrequnecies.setInt(lastID, frequency.getInt(index));
						lastIndexOnLists++;
						lastID++;
					}
					// add doc to posting list
					docExistsInPosting = false;
					if(listEnds.getInt(ID.getInt(index)) >= doc)
						docExistsInPosting = true;

					if(!docExistsInPosting)
					{
						listEnds.setInt(ID.getInt(index), doc);
						lastIndexOnLists++;
						frequency.setInt(index, frequency.getInt(index)+1);
						documentFrequnecies.setInt(ID.getInt(index), frequency.getInt(index));
					}
					return ID.getInt(index);
				}
				
//				if(n.equalKid == null)
				if(equalKidIndex.getInt(index) == 0)
				{
					numberOfNodes++;
					equalKidIndex.setInt(index, lastIndex);
					character.setByte(lastIndex, (byte)s.charAt(i+1));
					lastIndex++;
				}
				index = equalKidIndex.getInt(index);
				i++;
			}	
		}
		return -1;
	}
	
	public int getID(String s)
	{
		int i = 0;
		int stringSize = s.length();
		int index = 1;
		byte nodeChar = character.getByte(index);
		while(nodeChar != 0 && i < stringSize)
		{
			if(s.charAt(i) < nodeChar)
				index = loKidIndex.getInt(index);
			else if(s.charAt(i) > nodeChar)
				index = hiKidIndex.getInt(index);
			else
			{
				if(i == stringSize - 1 && ID.getInt(index) != 0)
					return ID.getInt(index);

				index = equalKidIndex.getInt(index);
				i++;
			}
			nodeChar = character.getByte(index);
		}
		return 0;
	}

	public int getFrequency(int ID)
	{
		return documentFrequnecies.getInt(ID);
	}
	
	public int getFrequency(String s)
	{
		int i = 0;
		int stringSize = s.length();
		int index = 1;
		byte nodeChar = character.getByte(index);
		while(nodeChar != 0 && i < stringSize)
		{
			if(s.charAt(i) < nodeChar)
				index = loKidIndex.getInt(index);
			else if(s.charAt(i) > nodeChar)
				index = hiKidIndex.getInt(index);
			else
			{
				if(i == stringSize - 1 && ID.getInt(index) != 0)
					return frequency.getInt(index);

				index = equalKidIndex.getInt(index);
				i++;
			}
			nodeChar = character.getByte(index);
		}
		return 0;
	}

	public void setPath(String path) throws IOException
	{
		this.path = path + "\\" + fileName;
		if(dictionaryFile != null)
			dictionaryFile.close();
		if(writer != null)
			writer.close();
		new File(this.path).delete();
		dictionaryFile = new RandomAccessFile(this.path, "rw");
		writer = dictionaryFile.getChannel();
	}
	
	public void writeToFile() throws IOException
	{
		writeBuffer.put((byte)0).putInt(0);
		writeBuffer.putInt(0).putInt(0).putInt(0);
		//write TST
		for(int i = 1; i < lastIndex; i++)
		{
			if(writeBuffer.remaining() < BLOCK_SIZE)
			{
				//write to file
				writeBuffer.flip();
				writer.write(writeBuffer);
				writeBuffer.clear();
			}
			writeBuffer.put(character.getByte(i)).putInt(ID.getInt(i));
			writeBuffer.putInt(loKidIndex.getInt(i)).putInt(equalKidIndex.getInt(i)).putInt(hiKidIndex.getInt(i));
		}
		writeBuffer.flip();
		writer.write(writeBuffer);
		writeBuffer.clear();
		
		//write frequencies
		frequenciesStartInFile = dictionaryFile.getFilePointer();
		writeBuffer.putInt(0);
		for(int i = 1; i < lastID; i++)
		{
			if(writeBuffer.remaining() < BLOCK_SIZE)
			{
				//write to file
				writeBuffer.flip();
				writer.write(writeBuffer);
				writeBuffer.clear();
			}
			writeBuffer.putInt(documentFrequnecies.getInt(i));
		}
		writeBuffer.flip();
		writer.write(writeBuffer);
		writeBuffer.clear();
		
		clear();
	}
	
	public int getIDfromFile(String s) throws IOException
	{
		int i = 0;
		int stringSize = s.length();
		int index = 1;
		dictionaryFile.seek(index*BLOCK_SIZE);
		byte nodeChar = dictionaryFile.readByte();
		while(nodeChar != 0 && i < stringSize && dictionaryFile.getFilePointer() < frequenciesStartInFile)
		{
			int ID = dictionaryFile.readInt();
			if(s.charAt(i) < nodeChar)
			{
//				index = loKidIndex.getInt(index);
				dictionaryFile.seek(index*BLOCK_SIZE+CHARACTER_SIZE+ID_SIZE);
				index = dictionaryFile.readInt();
			}
			else if(s.charAt(i) > nodeChar)
			{
//				index = hiKidIndex.getInt(index);
				dictionaryFile.seek(index*BLOCK_SIZE+CHARACTER_SIZE+ID_SIZE+LO_KID_INDEX_SIZE+EQUAL_KID_INDEX_SIZE);
				index = dictionaryFile.readInt();
			}
			else
			{
				if(i == stringSize - 1 && ID != 0)
				{
					return ID;
				}

//				index = equalKidIndex.getInt(index);
				dictionaryFile.seek(index*BLOCK_SIZE+CHARACTER_SIZE+ID_SIZE+LO_KID_INDEX_SIZE);
				index = dictionaryFile.readInt();
				i++;
			}
//			nodeChar = character.getByte(index);
			dictionaryFile.seek(index*BLOCK_SIZE);
			nodeChar = dictionaryFile.readByte();
		}
		return 0;

	}

	public int getFrequencyFromFile(int wordID) throws IOException
	{
		if(frequenciesStartInFile+wordID*FREQUENCY_SIZE >= dictionaryFile.length())
			return 1;
		dictionaryFile.seek(frequenciesStartInFile+wordID*FREQUENCY_SIZE);
		return dictionaryFile.readInt();
	}
	
	public void clearPostingsLists()
	{
		listEnds = new Train(1, Train.Type.BYTE);
	}
	
	public void clear()
	{
		character = new Train(1, Train.Type.BYTE);
		ID = new Train(1, Train.Type.INTEGER);
		frequency = new Train(1, Train.Type.INTEGER);
		loKidIndex = new Train(1, Train.Type.INTEGER);
		hiKidIndex = new Train(1, Train.Type.INTEGER);
		equalKidIndex = new Train(1, Train.Type.INTEGER);
		listEnds = new Train(1, Train.Type.INTEGER);
		documentFrequnecies = new Train(1, Train.Type.INTEGER);
	}
	
	public void shutdown() throws IOException
	{
		if(dictionaryFile != null)
			dictionaryFile.close();
		if(writer != null)
			writer.close();
		if(path != null)
			new File(this.path).delete();	
	}
	
	public static void main(String[] args) throws IOException
	{
		TrainTST at = new TrainTST();
		at.add("armin", 1);
		at.add("sina", 1);
		at.add("nojan", 1);
		at.add("pooya", 1);
		
		at.add("pegah", 2);
		at.add("melica", 2);
		at.add("armin", 2);
		at.add("sina", 2);
		
		at.add("armin", 3);
		at.add("pegah", 3);
		
		at.add("armin", 4);
		at.add("melica", 4);
		
		at.add("armin", 5);
		at.add("armin", 5);
		at.add("armin", 5);
		at.add("pooya", 5);
		
		at.add("armin", 6);
		at.add("sina", 6);

		System.out.println(at.getFrequency("armin"));
		
		for(int i = 1; i < 6; i++)
			System.out.println(i+"\t"+at.getFrequency(i));

		at.setPath("C:/Users/maxi/Documents/AUT/Information Storage and Retrieval/Projects/IR9293");
		at.writeToFile();
		
		System.out.println();
		System.out.println(at.getIDfromFile("pegah"));
		for(int i = 1; i < 6; i++)
			System.out.println(i+"\t"+at.getFrequencyFromFile(i));		
	}
}
