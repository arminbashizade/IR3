package read;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Scanner;
import java.util.StringTokenizer;

import stemmer.PaiceStemmer;
import dictionary.TrainTST;

/**
 *
 * @author Melica
 */

public class MyTokenizer
{
	public static final String MAIN_FILE = "C:/Users/maxi/Documents/AUT/Information Storage and Retrieval/Projects/IR9293/Main.dat";
	public static final String TEST_FILE = "C:/Users/maxi/Desktop/Dropbox/AUT/Information Storage and Retrieval/IR2/test.dat"; 
	public static final int BUFF_SIZE = 64*1024;
	String fileName;
	StringTokenizer tokenizer;
	ByteBuffer buff;
	String delim;
	byte[] bytes;
	int buffSize;
	RandomAccessFile  main;
	FileChannel reader;
	String bytesString;
	private long lastDocPos = 0;
	int indexInTokenizer = 0;
	int currDocNum = 0;
	private boolean endOfFile = false;
	public int buffNum = 1;
	public long fileSize;
	long[] docStartPosition = new long[50000];

	byte[] nextTokenByteArray = new byte[100];
	int nextTokenLength;
	int nextTokenDoc;
	
	public MyTokenizer(String fileName , String delim, int buffSize) throws FileNotFoundException
	{
		lastDocPos = 0;
		indexInTokenizer = 0;
		currDocNum = 0;
		endOfFile = false;
		buffNum = 1;
		
		this.buffSize = buffSize;
		bytes = new byte[buffSize];
		this.fileName = fileName;
		fileSize = new File(fileName).length();
		this.delim = delim;
		main = new RandomAccessFile(this.fileName, "r");
		reader = main.getChannel();
		buff = ByteBuffer.allocate(buffSize);
		buff.clear();
		try {
			endOfFile = reader.read(buff) > 0 ? false : true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		buff.flip();
		try{
			buff.get(bytes);
		}catch (BufferUnderflowException e){
			for(int i = 0; i < buff.limit(); i++)
				bytes[i] = buff.get();
		}
		bytesString = new String(bytes, 0, buff.limit());

		tokenizer = new StringTokenizer(bytesString, this.delim, true);
		indexInTokenizer = 0;
	}

	public TokenDocType getNextToken() throws IOException
	{
		TokenDocType tokenDoc;
		tokenDoc = new TokenDocType();
		String temp1 = delim;
		String temp2 = delim;
		while(tokenizer.hasMoreTokens() && delim.contains(temp1) && !temp1.equals("<")) //this line used to be : " if ( tokenizer.hasMoreTokens() ) " 
		{
			temp1 = tokenizer.nextToken();
			indexInTokenizer += temp1.length();
			if(temp1.equals("<")) // check this
			{
				if(tokenizer.hasMoreTokens())
				{
					temp1 = tokenizer.nextToken();
					indexInTokenizer += temp1.length();
				}
				else
				{
					buffNum++;
					buff.clear();
					if(reader.read(buff) > 0)
					{
						buff.flip();
						try{
							buff.get(bytes);
						}catch (BufferUnderflowException e){
							for(int i = 0; i < buff.limit(); i++)
								bytes[i] = buff.get();
						}
						bytesString = new String(bytes, 0, buff.limit());
						tokenizer = new StringTokenizer(bytesString, delim, true);
						indexInTokenizer = 0;
						temp1 = tokenizer.nextToken();
						indexInTokenizer += temp1.length();
						endOfFile = false;
					}
					else
						endOfFile = true;
				}
				
				if(temp1.equals("p") || temp1.equals("P"))
				{
					temp2 = new String(temp1);
					if(tokenizer.hasMoreTokens())
					{
						temp1 = tokenizer.nextToken();
						indexInTokenizer += temp1.length();
					}
					else
					{
						buffNum++;
						buff.clear();
						if(reader.read(buff) > 0)
						{
							buff.flip();
							try{
								buff.get(bytes);
							}catch (BufferUnderflowException e){
								for(int i = 0; i < buff.limit(); i++)
									bytes[i] = buff.get();
							}
							bytesString = new String(bytes, 0, buff.limit());
							tokenizer = new StringTokenizer(bytesString, delim, true);
							indexInTokenizer = 0;
							temp1 = tokenizer.nextToken();
							indexInTokenizer += temp1.length();
							endOfFile = false;
						}
						else
							endOfFile = true;
					}

					if(temp1.equals(">"))
					{
						lastDocPos = main.getFilePointer()-buff.limit()+indexInTokenizer;
						currDocNum++;
					}
					else
					{
						tokenDoc.token = temp2;
						tokenDoc.doc = currDocNum;
						return tokenDoc;
					}
				}
			}
		}

		if (!tokenizer.hasMoreTokens())
		{
			buffNum++;
			buff.clear();
			if(reader.read(buff) > 0)
			{
				buff.flip();
				try{
					buff.get(bytes);
				}catch (BufferUnderflowException e){
					for(int i = 0; i < buff.limit(); i++)
						bytes[i] = buff.get();
				}
				bytesString = new String(bytes, 0, buff.limit());
				tokenizer = new StringTokenizer(bytesString, delim, true);
				indexInTokenizer = 0;
				temp2 = tokenizer.nextToken();
				indexInTokenizer += temp2.length();
				endOfFile = false;
			}
			else
			{
				endOfFile = true;
				tokenDoc.token = temp1;
				tokenDoc.doc = currDocNum;
				return tokenDoc;

				//we should end the whole reading from file thing
				//we should also return temp1 as a valid token
			}

			if(delim.contains(new String(bytes, 0, 1)))
			{
				tokenDoc.token = temp1;
				tokenDoc.doc = currDocNum;
				return tokenDoc;
			}
			else
			{
				if(delim.contains(temp1))
					temp1 = "";
				tokenDoc.token = temp1 + temp2;
				tokenDoc.doc = currDocNum;
				return tokenDoc;
			}
		}
		tokenDoc.token = temp1;
		tokenDoc.doc = currDocNum;
		return tokenDoc;
	}

	public void getTokens() throws IOException
	{
		getNextToken();
		reader.close();
		main.close();
	}

	public boolean hasMoreTokens()
	{
		return !endOfFile ;
	}

	public long getLastDocPos()
	{
		return lastDocPos;
	}
	
	public void close() throws IOException
	{
		reader.close();
		main.close();
	}
	
	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException, InterruptedException 
	{
		int ARRAY_TRIE_ITERATIVE_OPTIMIZED = 0;
		int ARRAY_TRIE_RECURSIVE_OPTIMIZED = 1;
		int TREE_TRIE = 2;
		int TRAIN_TRIE = 3;

		//////////////////////////////////
		boolean runGC = false;
		int gcPeriod = 1000000;
		int ds = TRAIN_TRIE;
		String fileMode = MAIN_FILE;
		int indexMode = DocWordIndex.TEST_FILE;
		//////////////////////////////////
		
		Runtime runtime = Runtime.getRuntime();
		long startTime = System.currentTimeMillis();

		MyTokenizer m = new MyTokenizer(fileMode, "!@#$%^&*()_-=+\r \n?:{}|./,;\\'[]'\"<>", BUFF_SIZE);
		DocWordIndex docIndex = new DocWordIndex("testdocindex", indexMode);
		PaiceStemmer stemmer = new PaiceStemmer(PaiceStemmer.RULES, "");
		StopWords stopword = new StopWords();

		Normalizer.initialize();

		if(ds == TRAIN_TRIE)
		{
			//TrainTrie test iterative
			TrainTST at = new TrainTST();
			int word = 0;
			while(m.hasMoreTokens())
			{
				word++;
				if(runGC && word%gcPeriod == 0)
					System.gc();
//				if(word%10000000 == 0)
//				{
//					System.out.println(word+" words added, "+at.numberOfNodes+" nodes, "+(at.numberOfNodes*at.TOKEN_SIZE_BIT/8/1024)+" KB.");
//				}
				TokenDocType td = m.getNextToken();
				String s = Normalizer.removeAccents(td.token).toLowerCase();
				if(!stopword.isStopWord(s))
				{
					s = stemmer.stripAffixes(s);
					at.add(s, td.doc);
				}
//				docIndex.add(td.doc, at.search(s));
			}

			docIndex.close();
			System.out.println(word+" wrods.");
			System.out.println(at.lastID+" distinct words.");
			long time = System.currentTimeMillis() - startTime;
			long allocatedMemory = runtime.totalMemory();
			long freeMemory = runtime.freeMemory();
			long usingMemory = allocatedMemory - freeMemory;
			usingMemory /= 1024*1024;
			System.out.println("Memory usage:\t"+usingMemory+" MB.");
			System.out.println("Time:\t"+((double)time/1000.0)+" sec.");
			System.out.println("Trie size:\t\t"+(at.numberOfNodes)+" nodes.");

			m.close();
			System.gc();
			Scanner scanner = new Scanner(System.in);
			System.out.println("Enter words: (0 to finish)");
			String in = "1";
			while(!"0".equals(in))
			{
				in = scanner.nextLine();
				startTime = System.currentTimeMillis();

				System.out.println(at.getID(stemmer.stripAffixes(in)));

				time = System.currentTimeMillis() - startTime;
				System.out.println("Search Time:\t"+time+" milisec.");

				allocatedMemory = runtime.totalMemory();
				freeMemory = runtime.freeMemory();
				usingMemory = allocatedMemory - freeMemory;
				usingMemory /= 1024*1024;

				System.out.println("Memory usage:\t"+usingMemory+" MB.");
				System.out.println("Trie size:\t"+(at.numberOfNodes)+" nodes.");
				System.gc();
			}
			scanner.close();
		}
//		else if(ds == ARRAY_TRIE_ITERATIVE_OPTIMIZED)
//		{
//			//ArrayTrieIterativeOptimized test separate arrays
//			ArrayTrieIterativeOptimized at = new ArrayTrieIterativeOptimized();
//			int word = 0;
//			while(m.hasMoreTokens())
//			{
//				word++;
//				if(runGC && word%gcPeriod == 0)
//					System.gc();
////				if(word%10000000 == 0)
////				{
////					System.out.println(word+" words added, "+at.numberOfNodes+" nodes, "+(at.numberOfNodes*at.TOKEN_SIZE_BIT/8/1024)+" KB.");
////				}
//				String s = Normalizer.removeAccents(m.getNextToken().token).toLowerCase();
//				if(!stopword.isStopWord(s))
//					at.add(s);
//			}
//
//			System.out.println(word+" wrods.");
//			long time = System.currentTimeMillis() - startTime;
//			long allocatedMemory = runtime.totalMemory();
//			long freeMemory = runtime.freeMemory();
//			long usingMemory = allocatedMemory - freeMemory;
//			usingMemory /= 1024*1024;
//			System.out.println("Memory usage:\t"+usingMemory+" MB.");
//			System.out.println("Time:\t"+((double)time/1000.0)+" sec.");
//			System.out.println("Trie size:\t"+(at.numberOfNodes)+" nodes.");
//
//			m.close();
//			System.gc();
//			Scanner scanner = new Scanner(System.in);
//			System.out.println("Enter words: (0 to finish)");
//			String in = "1";
//			while(!"0".equals(in))
//			{
//				in = scanner.nextLine();
//				startTime = System.currentTimeMillis();
//
//				System.out.println(at.search(in));
//
//				time = System.currentTimeMillis() - startTime;
//				System.out.println("Search Time:\t"+time+" milisec.");
//
//				allocatedMemory = runtime.totalMemory();
//				freeMemory = runtime.freeMemory();
//				usingMemory = allocatedMemory - freeMemory;
//				usingMemory /= 1024*1024;
//
//				System.out.println("Memory usage:\t"+usingMemory+" MB.");
//				System.out.println("Trie size:\t"+(at.numberOfNodes)+" nodes.");
//				System.gc();
//			}
//			scanner.close();
//		}
//		else if(ds == ARRAY_TRIE_RECURSIVE_OPTIMIZED)
//		{
//		//ArrayTrie test recursive separate arrays
//			ArrayTrie at = new ArrayTrie();
//			int word = 0;
//			while(m.hasMoreTokens())
//			{
//				word++;
//				if(runGC && word%gcPeriod == 0)
//					System.gc();
////			if(word%10000000 == 0)
////			{
////				System.out.println(word+" words added, "+at.numberOfNodes+" nodes, "+(at.numberOfNodes*at.TOKEN_SIZE_BIT/8/1024)+" KB.");
////			}
//				String s = Normalizer.removeAccents(m.getNextToken().token).toLowerCase();
//				if(!stopword.isStopWord(s))
//					at.add(s);
//			}
//
//			System.out.println(word+" wrods.");
//			long time = System.currentTimeMillis() - startTime;
//			long allocatedMemory = runtime.totalMemory();
//			long freeMemory = runtime.freeMemory();
//			long usingMemory = allocatedMemory - freeMemory;
//			usingMemory /= 1024*1024;
//			System.out.println("Memory usage:\t"+usingMemory+" MB.");
//			System.out.println("Time:\t"+((double)time/1000.0)+" sec.");
//			System.out.println("Trie size:\t"+(at.numberOfNodes)+" nodes.");
//
//			m.close();
//			System.gc();
//			Scanner scanner = new Scanner(System.in);
//			System.out.println("Enter words: (0 to finish)");
//			String in = "1";
//			while(!"0".equals(in))
//			{
//				in = scanner.nextLine();
//				startTime = System.currentTimeMillis();
//
//				System.out.println(at.search(in));
//
//				time = System.currentTimeMillis() - startTime;
//				System.out.println("Search Time:\t"+time+" milisec.");
//
//				allocatedMemory = runtime.totalMemory();
//				freeMemory = runtime.freeMemory();
//				usingMemory = allocatedMemory - freeMemory;
//				usingMemory /= 1024*1024;
//
//				System.out.println("Memory usage:\t"+usingMemory+" MB.");
//				System.out.println("Trie size:\t"+(at.numberOfNodes)+" nodes.");
//				System.gc();
//			}
//			scanner.close();
//		}
//		else if(ds == TREE_TRIE)
//		{
//			//TernaryTrie test
//			TernaryTrie tst = new TernaryTrie();
//			int word = 0;
//			while(m.hasMoreTokens())
//			{
//				word++;
//				if(runGC && word%gcPeriod == 0)
//					System.gc();
//				if(word%10000000 == 0)
//				{
//					System.out.println(word+" words added, "+tst.numberOfNodes+" nodes.");
//				}
//				String s = Normalizer.removeAccents(m.getNextToken().token).toLowerCase();
//				if(!stopword.isStopWord(s))
//					tst.add(s, 0);
//			}
//	
//			System.out.println(word+" words.");
//			long time = System.currentTimeMillis() - startTime;
//			long allocatedMemory = runtime.totalMemory();
//			long freeMemory = runtime.freeMemory();
//			long usingMemory = allocatedMemory - freeMemory;
//			usingMemory /= 1024*1024;
//			System.out.println("Memory usage:\t"+usingMemory+" MB.");
//			System.out.println("Time:\t"+((double)time/1000.0)+" sec.");
//			System.out.println("Trie size:\t"+(tst.numberOfNodes)+" nodes.");
//			System.out.println("Max frequency:\t" + tst.maxFrequency);
//			System.out.println("Number of distinct words:\t"+tst.numberOfWords);
//	
//			System.gc();
//			Scanner scanner = new Scanner(System.in);
//			System.out.println("Enter words: (0 to finish)");
//			String in = "1";
//			while(!"0".equals(in))
//			{
//				in = scanner.nextLine();
//				startTime = System.currentTimeMillis();
//				
//				System.out.println(tst.search(in));
//				
//				time = System.currentTimeMillis() - startTime;
//				System.out.println("Time:\t"+time+" milisec.");
//				
//				allocatedMemory = runtime.totalMemory();
//				freeMemory = runtime.freeMemory();
//				usingMemory = allocatedMemory - freeMemory;
//				usingMemory /= 1024*1024;
//				
//				System.out.println("Memory usage:\t"+usingMemory+" MB.");
//				System.out.println("Time:\t"+((double)time/1000.0)+" sec.");
//				System.out.println("Trie size:\t"+(tst.numberOfNodes)+" nodes.");
//			}
//			scanner.close();
//		}
		//ArrayTrieRecursive test
//		ArrayTrieRecursive at = new ArrayTrieRecursive();
//		int word = 0;
//		while(m.hasMoreTokens())
//		{
//			word++;
////			if(word%100000 == 0)
////				System.gc();
////			if(word%10000000 == 0)
////			{
////				System.out.println(word+" words added, "+at.numberOfNodes+" nodes, "+(at.numberOfNodes*at.TOKEN_SIZE_BIT/8/1024)+" KB.");
////			}
//			String s = Normalizer.removeAccents(m.getNextToken().token).toLowerCase();
//			if(!StopWords.isStopWord(s))
//				at.add(s);
//		}
//
//		System.out.println(word+" wrods.");
//		long time = System.currentTimeMillis() - startTime;
//		long allocatedMemory = runtime.totalMemory();
//		long freeMemory = runtime.freeMemory();
//		long usingMemory = allocatedMemory - freeMemory;
//		usingMemory /= 1024*1024;
//		System.out.println("Memory usage:\t"+usingMemory+" MB.");
//		System.out.println("Time:\t"+((double)time/1000.0)+" sec.");
//		System.out.println("Trie size:\t"+(at.numberOfNodes)+" nodes.");
//		
//		m.close();
//		System.gc();
//		Scanner scanner = new Scanner(System.in);
//		System.out.println("Enter words: (0 to finish)");
//		String in = "1";
//		while(!"0".equals(in))
//		{
//			in = scanner.nextLine();
//			startTime = System.currentTimeMillis();
//
//			System.out.println(at.search(in));
//
//			time = System.currentTimeMillis() - startTime;
//			System.out.println("Search Time:\t"+time+" milisec.");
//
//			allocatedMemory = runtime.totalMemory();
//			freeMemory = runtime.freeMemory();
//			usingMemory = allocatedMemory - freeMemory;
//			usingMemory /= 1024*1024;
//
//			System.out.println("Memory usage:\t"+usingMemory+" MB.");
//			System.out.println("Trie size:\t"+(at.numberOfNodes)+" nodes.");
//			System.gc();
//		}
//		scanner.close();

		//ArrayTrieIterative test
//		ArrayTrieIterative at = new ArrayTrieIterative();
//		int word = 0;
//		while(m.hasMoreTokens())
//		{
//			word++;
////			if(word%100000 == 0)
////				System.gc();
////			if(word%10000000 == 0)
////			{
////				System.out.println(word+" words added, "+at.numberOfNodes+" nodes, "+(at.numberOfNodes*at.TOKEN_SIZE_BIT/8/1024)+" KB.");
////			}
//			String s = Normalizer.removeAccents(m.getNextToken().token).toLowerCase();
//			if(!StopWords.isStopWord(s))
//				at.add(s);
//		}
//
//		System.out.println(word+" wrods.");
//		long time = System.currentTimeMillis() - startTime;
//		long allocatedMemory = runtime.totalMemory();
//		long freeMemory = runtime.freeMemory();
//		long usingMemory = allocatedMemory - freeMemory;
//		usingMemory /= 1024*1024;
//		System.out.println("Memory usage:\t"+usingMemory+" MB.");
//		System.out.println("Time:\t"+((double)time/1000.0)+" sec.");
//		System.out.println("Trie size:\t"+(at.numberOfNodes)+" nodes.");
//		
//		m.close();
//		System.gc();
//		Scanner scanner = new Scanner(System.in);
//		System.out.println("Enter words: (0 to finish)");
//		String in = "1";
//		while(!"0".equals(in))
//		{
//			in = scanner.nextLine();
//			startTime = System.currentTimeMillis();
//
//			System.out.println(at.search(in));
//
//			time = System.currentTimeMillis() - startTime;
//			System.out.println("Search Time:\t"+time+" milisec.");
//
//			allocatedMemory = runtime.totalMemory();
//			freeMemory = runtime.freeMemory();
//			usingMemory = allocatedMemory - freeMemory;
//			usingMemory /= 1024*1024;
//
//			System.out.println("Memory usage:\t"+usingMemory+" MB.");
//			System.out.println("Trie size:\t"+(at.numberOfNodes)+" nodes.");
//			System.gc();
//		}
//		scanner.close();
			
		
		//FileTrie test
//		FileTrieOptimized trie = new FileTrieOptimized("test16", FileTrieOptimized.TEST, FileTrieOptimized.KEEP);
//		int i = 0;
////		RandomAccessFile words = new RandomAccessFile(FileTrieOptimized.TEST_OUTPUT_DIRECTORY+"words.txt", "rw");
//		while(m.hasMoreTokens())
//		{
//			i++;
//			if(i%100000 == 0)
//			{
//				System.out.println(i+"\t"+m.currDocNum);
//			}
//			String s = Normalizer.removeAccents(m.getNextToken().token).toLowerCase();
//			if(!StopWords.isStopWord(s))
//			{
//				try{
////					System.out.println(s);
////					words.writeBytes(s+"\n");
////					trie.add(s, 0);
//					trie.addWithBuffer(s, 0);
//				}catch(StackOverflowError e){
//					System.err.println("Time:\t"+((System.currentTimeMillis()-startTime)/1000)+" sec.");
//					System.err.println("Stack Overflow!!");
//					trie.output();
//					System.exit(1);
//				}catch(BufferOverflowException e){
//					System.err.println("Time:\t"+((System.currentTimeMillis()-startTime)/1000)+" sec.");
//					System.err.println("Buffer Overflow!!");
//					e.printStackTrace();
//					trie.output();
//					System.exit(1);
//				}
//			}
//		}
//		int time = (int) (System.currentTimeMillis()-startTime);
//		System.out.println("Time:\t"+(time/1000)+" sec.");
////		words.close();
//		System.out.println(m.currDocNum+" documents, "+i+" words.");
//		trie.close();
//		trie.output();
//		System.out.println(trie.search("mckeo wn"));
		
		m.close();
	}

}