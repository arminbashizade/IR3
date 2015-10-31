package vector;

import java.awt.Cursor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import dictionary.TrainTST;
import read.DocWordIndex;
import read.MyTokenizer;
import read.TrainDocWordIndex;

public class DocVectorCalculator
{
	private static final int FLOAT_SIZE = 4;
	private static final int INTEGER_SIZE = 4;
	private static final int BUFFER_SIZE = MyTokenizer.BUFF_SIZE;
	RandomAccessFile vectorFile;
	FileChannel writer;
	ByteBuffer buffer;
	String path;
	String fileName;
	
	ArrayList<Long> docPositionInVectorFile;
	DocWordIndex docTerm;
	TrainDocWordIndex trainDocTerm;
	TrainTST termDoc;
	int totalDoc;
	
	public DocVectorCalculator(String fileName, TrainTST termDoc, DocWordIndex docTerm, int totalDoc)
	{
		trainDocTerm = null;
		buffer = ByteBuffer.allocate(BUFFER_SIZE);
		docPositionInVectorFile = new ArrayList<Long>();
		docPositionInVectorFile.add((long)-1);// first doc index is 1
		
		this.fileName = fileName;
		this.docTerm = docTerm;
		this.termDoc = termDoc;
		this.totalDoc = totalDoc;
	}
	public DocVectorCalculator(String fileName, TrainTST termDoc, TrainDocWordIndex docTerm, int totalDoc)
	{
		this.docTerm = null;
		buffer = ByteBuffer.allocate(BUFFER_SIZE);
		docPositionInVectorFile = new ArrayList<Long>();
		docPositionInVectorFile.add((long)-1);// first doc index is 1
		
		this.fileName = fileName;
		this.trainDocTerm = docTerm;
		this.termDoc = termDoc;
		this.totalDoc = totalDoc;
	}
	
	public void setPath(String path) throws FileNotFoundException
	{
		this.path = path + "\\" + fileName;
		new File(this.path).delete();
		vectorFile = new RandomAccessFile(this.path, "rw");
		writer = vectorFile.getChannel();
	}
	public class Task extends SwingWorker<Void, Void> {
		/*
		 * Main task. Executed in background thread.
		 */

		private long startTime;
		private JTextArea resultArea;
		private JButton selectMainFile;
		private JButton selectStemminRules;
		private JButton processFileButton;
		private JButton searchButton;
		private JTextField searchField;
		private Cursor searchFieldCursor;
		private JFrame frame;
		public Task(long startTime, JTextArea resultArea, JButton selectMainFile,
				JButton selectStemmingRules, JButton processFileButton, JButton searchButton,
				JTextField searchField, Cursor searchFieldCursor, JFrame frame)
		{
			this.startTime = startTime;
			this.resultArea = resultArea;
			this.selectMainFile = selectMainFile;
			this.selectStemminRules = selectStemmingRules;
			this.processFileButton = processFileButton;
			this.searchButton = searchButton;
			this.searchField = searchField;
			this.searchFieldCursor = searchFieldCursor;
			this.frame = frame;
		}
		
		@Override
		public Void doInBackground() throws IOException
		{
			if(trainDocTerm == null)
			{
				int fileSize = 0;
				setProgress(0);
				ArrayList<Float> weight;
				ArrayList<Integer> docWords;
				ArrayList<Integer> docWordsFrequencies;
				int df, tf, maxTF = 0;
				int id;
				int size;
				for(int i = 1; i <= totalDoc; i++)
				{
					setProgress((int) ((float)i/(float)totalDoc*100));
					weight = new ArrayList<Float>();
					docWords = docTerm.getWords(i);
					docWordsFrequencies = docTerm.getFrequencies(i);
					size = docWords.size();
					for(int j = 0; j < size; j++)
					{
						id = docWords.get(j);
						df = termDoc.getFrequency(id);
						tf = docWordsFrequencies.get(j);
						if(maxTF < tf)
							maxTF = tf;
						weight.add((float)tf*((float)Math.log10(totalDoc)-(float)Math.log10(df)));
					}

					for(int j = 0; j < size; j++)
						weight.set(j, weight.get(j)/(float)maxTF);

					//write to file
					if(buffer.remaining() < size*(INTEGER_SIZE+FLOAT_SIZE)) // write to file
					{
						buffer.flip();
						writer.write(buffer);
						buffer.clear();

						if(buffer.capacity() < size*(INTEGER_SIZE+FLOAT_SIZE))
						{
							int n = 1;
							while(n*BUFFER_SIZE < size*(INTEGER_SIZE+FLOAT_SIZE))
								n++;
							buffer = ByteBuffer.allocate(n*BUFFER_SIZE);
							buffer.clear();
						}
					}

					docPositionInVectorFile.add(vectorFile.getFilePointer()+buffer.position());
					for(int j = 0; j < size; j++)
						buffer.putInt(docWords.get(j)).putFloat(weight.get(j));
					fileSize += size*(INTEGER_SIZE+FLOAT_SIZE);

	//				docPositionInVectorFile.add(vectorFile.getFilePointer());
	//				
	//				for(int j = 0; j < size; j++)
	//				{
	//					if(weight.get(j) == 0)
	//						continue;
	//					vectorFile.writeInt(docWords.get(j));
	//					vectorFile.writeFloat(weight.get(j));
	//				}
				}

				buffer.flip();
				writer.write(buffer);
				buffer.clear();

				System.out.println(fileSize);
				setProgress(100);
			}
			else
			{
				int fileSize = 0;
				setProgress(0);
				int[] docWords;
				int[] docWordsFrequencies;
				float[] weight;
//				ArrayList<Float> weight;
//				ArrayList<Integer> docWords;
//				ArrayList<Integer> docWordsFrequencies;
				int df, tf, maxTF = 0;
				int id;
				int size;
				for(int i = 1; i <= totalDoc; i++)
				{
					System.out.println(i);
					size = trainDocTerm.getSize(i);
					setProgress((int) ((float)i/(float)totalDoc*100));
					if(size == 0)
					{
						docPositionInVectorFile.add(vectorFile.getFilePointer()+buffer.position());
						continue;
					}
					weight = new float[size];
					docWords = trainDocTerm.getWords(i);
					docWordsFrequencies = trainDocTerm.getFrequencies(i);
					for(int j = 0; j < size; j++)
					{
						id = docWords[j];
						df = termDoc.getFrequency(id);
						tf = docWordsFrequencies[j];
						if(maxTF < tf)
							maxTF = tf;
						weight[j] = ((float)tf*((float)Math.log10(totalDoc)-(float)Math.log10(df)));
					}

					for(int j = 0; j < size; j++)
						weight[j] = weight[j]/(float)maxTF;

					//write to file
					if(buffer.remaining() < size*(INTEGER_SIZE+FLOAT_SIZE)) // write to file
					{
						buffer.flip();
						writer.write(buffer);
						buffer.clear();

						if(buffer.capacity() < size*(INTEGER_SIZE+FLOAT_SIZE))
						{
							int n = 1;
							while(n*BUFFER_SIZE < size*(INTEGER_SIZE+FLOAT_SIZE))
								n++;
							buffer = ByteBuffer.allocate(n*BUFFER_SIZE);
							buffer.clear();
						}
					}

					docPositionInVectorFile.add(vectorFile.getFilePointer()+buffer.position());
					for(int j = 0; j < size; j++)
						buffer.putInt(docWords[j]).putFloat(weight[j]);
					fileSize += size*(INTEGER_SIZE+FLOAT_SIZE);

//					docPositionInVectorFile.add(vectorFile.getFilePointer());
//					
//					for(int j = 0; j < size; j++)
//					{
//						if(weight.get(j) == 0)
//							continue;
//						vectorFile.writeInt(docWords.get(j));
//						vectorFile.writeFloat(weight.get(j));
//					}
				}

				buffer.flip();
				writer.write(buffer);
				buffer.clear();

				System.out.println("\t"+fileSize);
				setProgress(100);
			}
			return null;
		}
		/*
		 * Executed in event dispatching thread
		 */
		@Override
		public void done()
		{
			System.gc();
			long time = System.currentTimeMillis() - startTime;
			resultArea.append("Creating vectors time: "+((double)time/1000.0)+" sec.\n");
			resultArea.append("\n");
			resultArea.setCaretPosition(resultArea.getText().length());
			java.awt.Toolkit.getDefaultToolkit().beep();
			selectMainFile.setEnabled(true);
			selectStemminRules.setEnabled(true);
			processFileButton.setEnabled(true);
			searchButton.setEnabled(true);
			searchField.setCursor(searchFieldCursor);
			searchField.setEnabled(true);
			frame.setCursor(null); //turn off the wait cursor
		}
	}	

	public void createVectors() throws IOException
	{
		ArrayList<Float> weight;
		ArrayList<Integer> docWords;
		ArrayList<Integer> docWordsFrequencies;
		int df, tf, maxTF = 0;
		int id;
		int size;
		for(int i = 1; i <= totalDoc; i++)
		{
			weight = new ArrayList<Float>();
			docWords = docTerm.getWords(i);
			docWordsFrequencies = docTerm.getFrequencies(i);
			size = docWords.size();
			for(int j = 0; j < size; j++)
			{
				id = docWords.get(j);
				df = termDoc.getFrequency(id);
				tf = docWordsFrequencies.get(j);
				if(maxTF < tf)
					maxTF = tf;
				weight.add((float)tf*((float)Math.log10(totalDoc)-(float)Math.log10(df)));
			}
			
			for(int j = 0; j < size; j++)
				weight.set(j, weight.get(j)/(float)maxTF);

			docPositionInVectorFile.add(vectorFile.getFilePointer());
			
			for(int j = 0; j < size; j++)
			{
				if(weight.get(j) == 0)
					continue;
				vectorFile.writeInt(docWords.get(j));
				vectorFile.writeFloat(weight.get(j));
			}
		}
	}

	public void clear() throws IOException
	{
		if(vectorFile != null)
			vectorFile.close();
		if(path != null)
			new File(path).delete();
	}
}
