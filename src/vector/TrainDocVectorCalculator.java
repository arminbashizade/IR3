package vector;

import java.awt.Cursor;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import clustering.KMeans;
import dictionary.Train;
import dictionary.TrainTST;
import read.TrainDocWordIndex;

public class TrainDocVectorCalculator
{
	private static final int LARGE_WAGON_SIZE = 1000*1000;
	private static final int SMALL_WAGON_SIZE = 50*1000;
	private static final int LEADERS_TO_FOLLOW = 3;
	
	Train vectorWords = null;
	Train vectorWeights = null;
	Train startInList = null;
	int lastIndex = 0;
	
	TrainDocWordIndex trainDocTerm;
	TrainTST termDoc;
	KMeans clusters;
	int totalDoc;
	
	TrainDocVectorCalculator thisVectors;
	
	public TrainDocVectorCalculator(TrainTST termDoc, TrainDocWordIndex docTerm, int totalDoc, KMeans clustres)
	{
		thisVectors = this;
		lastIndex = 0;
		vectorWords = new Train(LARGE_WAGON_SIZE, Train.Type.INTEGER);
		vectorWeights = new Train(LARGE_WAGON_SIZE, Train.Type.DOUBLE);
		startInList = new Train(SMALL_WAGON_SIZE, Train.Type.INTEGER);
		this.trainDocTerm = docTerm;
		this.termDoc = termDoc;
		this.totalDoc = totalDoc;
		this.clusters = clustres;
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
		private PropertyChangeListener server;
		public Task(long startTime, JTextArea resultArea, JButton selectMainFile,
				JButton selectStemmingRules, JButton processFileButton, JButton searchButton,
				JTextField searchField, Cursor searchFieldCursor, JFrame frame, PropertyChangeListener server)
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
			this.server = server;
		}

		@Override
		public Void doInBackground() throws IOException
		{
			setProgress(0);
			int[] docWords;
			int[] docWordsFrequencies;
			float[] weight;

			int df, tf, maxTF = 0;
			int id;
			int size;
			for(int i = 1; i <= totalDoc; i++)
			{
				startInList.setInt(i, lastIndex);
				size = trainDocTerm.getSize(i);
				setProgress((int) ((float)i/(float)totalDoc*100));
				if(size == 0)
					continue;

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

					if(df == 0)
						df = 1;
					weight[j] = ((float)tf*((float)Math.log10(totalDoc)-(float)Math.log10(df)));
				}

				for(int j = 0; j < size; j++)
					weight[j] = weight[j]/(float)maxTF;

				for(int j = 0; j < size; j++)
				{
					if(weight[j] == 0)
						continue;
					vectorWords.setInt(lastIndex, docWords[j]);
					vectorWeights.setDouble(lastIndex, (double)weight[j]);
					lastIndex++;
				}
				
				//sort vectors for merging in cosine calculation
				float[] completeVector = new float[termDoc.lastID];
				for(int j = startInList.getInt(i); j < lastIndex; j++)
					completeVector[vectorWords.getInt(j)] = vectorWeights.getInt(j);
				
				int cnt = 0;
				for(int j = 0; j < termDoc.lastID; j++)
				{
					if(completeVector[j] != 0)
					{
						vectorWords.setInt(cnt, j);
						vectorWeights.setDouble(cnt, completeVector[j]);
						cnt++;
					}
				}
//				for(int j = startInList.getInt(i); j < lastIndex-1; j++)
//				{
//					int minIndex = j;
//					for(int k = j+1; k < lastIndex; k++)
//						if(vectorWords.getInt(minIndex) > vectorWords.getInt(k))
//							minIndex = k;
//					
//					if(minIndex != j)
//					{
//						int tmpInt = vectorWords.getInt(j);
//						float tmpFloat = (float) vectorWeights.getDouble(j);
//						
//						vectorWords.setInt(j, vectorWords.getInt(minIndex));
//						vectorWeights.setDouble(j, vectorWeights.getDouble(minIndex));
//						
//						vectorWords.setInt(minIndex, tmpInt);
//						vectorWeights.setDouble(minIndex, tmpFloat);
//					}
//				}
			}

			startInList.setInt(totalDoc+1, lastIndex);
			setProgress(100);
			return null;
		}
		/*
		 * Executed in event dispatching thread
		 */
		@Override
		public void done()
		{
			long time = System.currentTimeMillis() - startTime;
			
			trainDocTerm.clear();
			System.gc();
			long allocatedMemory = Runtime.getRuntime().totalMemory();
			long freeMemory = Runtime.getRuntime().freeMemory();
			long usingMemory = allocatedMemory - freeMemory;
			usingMemory /= 1024*1024;
			resultArea.append("Memory usage: "+usingMemory+" MB.\n");
			time = System.currentTimeMillis() - startTime;
			resultArea.append("Creating vectors time: "+((double)time/1000.0)+" sec.\n");
			resultArea.append("\n");
			resultArea.setCaretPosition(resultArea.getText().length());

			////////////////////////////////////////
			try {
				termDoc.writeToFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.gc();
			////////////////////////////////////////

			//clustering
			clusters = new KMeans(termDoc.lastID, totalDoc, null, (int)Math.sqrt(totalDoc), LEADERS_TO_FOLLOW, thisVectors);
			startTime = System.currentTimeMillis();
			resultArea.append("Clustering...\n");
			resultArea.setCaretPosition(resultArea.getText().length());
			
			clustering.KMeans.Task task = clusters.new Task(startTime, resultArea, selectMainFile, selectStemminRules, processFileButton, searchButton, searchField, searchFieldCursor, frame);
			task.addPropertyChangeListener(server);
			task.execute();
		}
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
			result[i] = vectorWords.getInt(offset+i);
		
		return result;
	}
	
	public float[] getWeights(int docID)
	{
		offset = startInList.getInt(docID);
		size = startInList.getInt(docID+1)-startInList.getInt(docID);
		float[] result = new float[size];
		
		for(i = 0; i < size; i++)
			result[i] = (float) vectorWeights.getDouble(offset+i);
		
		return result;
	}


	public int getSize(int docID)
	{
		return startInList.getInt(docID+1)-startInList.getInt(docID);
	}

	public KMeans getClusters()
	{
		return clusters;
	}
	
	public void clear()
	{
		vectorWords = new Train(1, Train.Type.INTEGER);
		vectorWeights = new Train(1, Train.Type.DOUBLE);
		startInList = new Train(1, Train.Type.INTEGER);
	}
}
