package clustering;

import java.awt.Cursor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import read.MyTokenizer;
import vector.CosineCalculator;
import vector.TrainDocVectorCalculator;

public class KMeans implements Comparator<IndexDistance>
{
	private static final int BUFFER_SIZE = MyTokenizer.BUFF_SIZE;
	private static final int CLUSTERING_MAX_ITTERATION = 3;
	private static final int INITIALIZING_MAX_ITERATION = 10;
	
	int numberOfDocs;
	int numberOfWords = 0;
	
	RandomAccessFile clustersFile;
	ByteBuffer buffer;
	FileChannel writer;
	String fileName;
	String path;
	
	public int K;
	int leadersToFollow;
	
	ArrayList<Long> leadersPositionInFile;
	ArrayList<Integer> leaderWords;
	ArrayList<Float> leaderWeights;
	
//	DocVector[] leaders;
	int[] nearestLeaderIndex;
	int[][] followingLeaders;
	public int[][] clusters;
	public int[] clusterSize;
	public int[][] leadersWords;
	public float[][] leadersWeights;
	public int[] leadersSize;
	TrainDocVectorCalculator vectors;
	Random rand = new Random(System.currentTimeMillis());
	
	KMeans thisKMeans;
	
	public KMeans(int numberOfWords, int numberOfDocs, String fileName, int K, int b1, TrainDocVectorCalculator vectors)
	{
		leadersPositionInFile = new ArrayList<Long>();
		buffer = ByteBuffer.allocate(BUFFER_SIZE);
		this.numberOfDocs = numberOfDocs;
		this.fileName = fileName;
		this.K = K;
		leadersToFollow = b1;
		leaderWords = new ArrayList<Integer>();
		leaderWeights = new ArrayList<Float>();
//		leaders = new DocVector[this.K];
		followingLeaders = new int[this.numberOfDocs+1][this.leadersToFollow];
		nearestLeaderIndex = new int[this.numberOfDocs+1];
		leadersWords = new int[this.K][];
		leadersWeights = new float[this.K][];
		leadersSize = new int[this.K];
		this.vectors = vectors;
		this.numberOfWords = numberOfWords;
		thisKMeans = this;
	}
	
	public void setPath(String path) throws FileNotFoundException
	{
		this.path = path + "\\" + fileName;
		new File(this.path).delete();
		clustersFile = new RandomAccessFile(this.path, "rw");
		writer = clustersFile.getChannel();
	}

	public class Task extends SwingWorker<Void, Void> {
		/*
		 * Main task. Executed in background thread.
		 */

		private static final float THRESHOLD = (float) 0.9;
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
			CosineCalculator cosine = new CosineCalculator();
			PriorityQueue<IndexDistance> currentVectorNearestLeaders = new PriorityQueue<IndexDistance>(K, thisKMeans);
			float[] tmpLeaderVector;
			int oldVectorSize;
			int[] oldVectorWords;
			float[] oldVectorWeights;
			int nonZeroElements = 0;
			int docVectorSize = 0;
			int[] docWords;
			float[] docWeights;
			int tmpLeaderVectorSize = 0;
			int[] tmpLearDocWords;
			float[] tmpLeaderDocWeights;
			
			float dist = 0;
			float maxDist = -2;
			int maxDistIndex = 0;
			float variance = 0;
			float average = 0;
			float bestVariance = 0;
			
			
			setProgress(0);
			int[] tmpLeadersID = new int[K];
			int[] initializingLeadersID = new int[K];
			int[] tmpNumberOfFollowers = new int[K];
			int r;
			for(int j = 0; j < INITIALIZING_MAX_ITERATION; j++)
			{
				for(int i = 0; i < K; i++)
				{
					tmpNumberOfFollowers[i] = 0;
					do
						r = rand.nextInt()%numberOfDocs;
					while(r < 1 || contains(tmpLeadersID, K, r));
					tmpLeadersID[i] = r;
				}
				for(int l = 1; l <= numberOfDocs; l++)
				{
					docVectorSize = vectors.getSize(l);
					docWords = vectors.getWords(l);
					docWeights = vectors.getWeights(l);

					for(int k = 0; k < K; k++)
					{
						tmpLeaderVectorSize = vectors.getSize(tmpLeadersID[k]);
						tmpLearDocWords = vectors.getWords(tmpLeadersID[k]);
						tmpLeaderDocWeights = vectors.getWeights(tmpLeadersID[k]);
						dist = cosine.cosineDistance(docWords, docWeights, docVectorSize, tmpLearDocWords, tmpLeaderDocWeights, tmpLeaderVectorSize);
						if(dist > maxDist)
						{
							maxDist = dist;
							maxDistIndex = k;
						}
					}
					tmpNumberOfFollowers[maxDistIndex]++;
				}
				variance = 0;
				average = 0;
				for(int i = 0; i < K; i++)
					average += tmpNumberOfFollowers[i];
				average /= (float)K;
				for(int i = 0; i < K; i++)
					variance += (tmpNumberOfFollowers[i]-average)*(tmpNumberOfFollowers[i]-average);
				
				variance /= K;
				if(variance < bestVariance)
				{
					bestVariance = variance;
					for(int i = 0; i < K; i++)
						initializingLeadersID[i] = tmpLeadersID[i];
				}
			}
			
			for(int i = 0; i < K; i++)
			{
//				leaders[i] = new DocVector(vectors, tmpLeadersID[i]);
				leadersWords[i] = vectors.getWords(initializingLeadersID[i]);
				leadersWeights[i] = vectors.getWeights(initializingLeadersID[i]);
				leadersSize[i] = vectors.getSize(initializingLeadersID[i]);
			}

			boolean changed = true;
			for(int i = 0; i < CLUSTERING_MAX_ITTERATION; i++)
			{
				System.out.println("Iteration:\t"+i);
				if(!changed)
				{
					break;
				}
				
				//assign vectors to leaders
				for(int j = 1; j <= numberOfDocs; j++)
				{
					setProgress((int) ((float)i/(float)CLUSTERING_MAX_ITTERATION*100 + (float)j/(float)numberOfDocs*(float)1/(float)CLUSTERING_MAX_ITTERATION*100));
					currentVectorNearestLeaders.clear();

					docVectorSize = vectors.getSize(j);
					docWords = vectors.getWords(j);
					docWeights = vectors.getWeights(j);
					
					for(int k = 0; k < K; k++)
					{
						dist = cosine.cosineDistance(docWords, docWeights, docVectorSize, leadersWords[k], leadersWeights[k], leadersSize[k]);
						currentVectorNearestLeaders.add(new IndexDistance(k, dist));
					}
					
					nearestLeaderIndex[j] = currentVectorNearestLeaders.peek().index;

					for(int k = 0; k < leadersToFollow; k++)
						followingLeaders[j][k] = currentVectorNearestLeaders.poll().index;
				}
				changed = false;
				//update leaders, check for change
				for(int k = 0; k < K; k++)
				{
					int numberOfFollowers = 0;
					tmpLeaderVector = new float[numberOfWords+1];
					for(int j = 1; j <= numberOfWords; j++)
						tmpLeaderVector[j] = 0;
					
					for(int j = 1; j <= numberOfDocs; j++)
					{
						if(nearestLeaderIndex[j] == k)
						{
							numberOfFollowers++;
							docVectorSize = vectors.getSize(j);
							docWords = vectors.getWords(j);
							docWeights = vectors.getWeights(j);
							
							for(int l = 0; l < docVectorSize; l++)
								tmpLeaderVector[docWords[l]] += docWeights[l];
						}
					}

					nonZeroElements = 0;
					for(int j = 1; j <= numberOfWords; j++)
						if(tmpLeaderVector[j] != 0)
							nonZeroElements++;
					
					oldVectorSize = leadersSize[k];
					oldVectorWords = new int[oldVectorSize];
					oldVectorWeights = new float[oldVectorSize];
					for(int j = 0; j < oldVectorSize; j++)
					{
						oldVectorWeights[j] = leadersWeights[k][j];
						oldVectorWords[j] = leadersWords[k][j];
					}
					
					leadersSize[k] = nonZeroElements;
					leadersWords[k] = new int[nonZeroElements];
					leadersWeights[k] = new float[nonZeroElements];
					
					int tmpIndex = 0;
					for(int j = 1; j <= numberOfWords; j++)
					{
						if(tmpLeaderVector[j] != 0)
						{
							leadersWords[k][tmpIndex] = j;
							leadersWeights[k][tmpIndex] = tmpLeaderVector[j]/(float)numberOfFollowers;
							tmpIndex++;
						}
					}

					//check for change
					if(!changed)
					{
						if(cosine.cosineDistance(oldVectorWords, oldVectorWeights, oldVectorSize, leadersWords[k], leadersWeights[k], leadersSize[k]) < THRESHOLD)
							changed = true;
//						if(oldVectorSize != leadersSize[k])
//							changed = true;
//						else
//						{
//							for(int j = 0; j < leadersSize[k]; j++)
//							{
//								if(leadersWords[k][j] != oldVectorWords[j] || leadersWeights[k][j] != oldVectorWeights[j])
//								{
//									changed = true;
//									break;
//								}
//							}
//						}
					}
				}
			}
		
			clusters = new int[K][];
			clusterSize = new int[K];
			for(int i = 1; i <= numberOfDocs; i++)
				for(int j = 0; j < leadersToFollow; j++)
					clusterSize[followingLeaders[i][j]]++;
			
			int[] lastIndex = new int[K];
			for(int i = 0; i < K; i++)
			{
				clusters[i] = new int[clusterSize[i]];
				lastIndex[i] = 0;
			}
			
			for(int i = 1; i <= numberOfDocs; i++)
			{
				for(int j = 0; j < leadersToFollow; j++)
				{
					clusters[followingLeaders[i][j]][lastIndex[followingLeaders[i][j]]] = i;
					lastIndex[followingLeaders[i][j]]++;
				}
			}

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
			resultArea.append("Clustering time: "+((double)time/1000.0)+" sec.\n");
			resultArea.setCaretPosition(resultArea.getText().length());
			System.gc();
			long allocatedMemory = Runtime.getRuntime().totalMemory();
			long freeMemory = Runtime.getRuntime().freeMemory();
			long usingMemory = allocatedMemory - freeMemory;
			usingMemory /= 1024*1024;
			resultArea.append("Memory usage: "+usingMemory+" MB.\n");
			
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

	
	public void cluster() throws IOException
	{
		int[] tmpLeadersID = new int[K];
		int r;
		
		for(int i = 0; i < K; i++)
		{
			do
				r = rand.nextInt()%numberOfDocs;
			while(r < 1 || contains(tmpLeadersID, K, r));
			tmpLeadersID[i] = r;
		}
		
		for(int i = 0; i < K; i++)
		{
//			leaders[i] = new DocVector(vectors, tmpLeadersID[i]);
			leadersWords[i] = vectors.getWords(tmpLeadersID[i]);
			leadersWeights[i] = vectors.getWeights(tmpLeadersID[i]);
			leadersSize[i] = vectors.getSize(tmpLeadersID[i]);
		}

		PriorityQueue<IndexDistance> currentVectorNearestLeaders = new PriorityQueue<IndexDistance>(this.K, this);
		CosineCalculator cosine = new CosineCalculator();
		float[] tmpLeaderVector;
		int oldVectorSize;
		int[] oldVectorWords;
		float[] oldVectorWeights;
		int nonZeroElements = 0;
		int docVectorSize = 0;
		int[] docWords;
		float[] docWeights;
		float dist = 0;
		
		boolean changed = true;
		for(int i = 0; i < CLUSTERING_MAX_ITTERATION; i++)
		{
			System.out.println("Iteration:\t"+i);
			if(!changed)
				break;
			
			//assign vectors to leaders
			for(int j = 1; j <= numberOfDocs; j++)
			{
				currentVectorNearestLeaders.clear();

				docVectorSize = vectors.getSize(j);
				docWords = vectors.getWords(j);
				docWeights = vectors.getWeights(j);
				
				for(int k = 0; k < K; k++)
				{
					dist = cosine.cosineDistance(docWords, docWeights, docVectorSize, leadersWords[k], leadersWeights[k], leadersSize[k]);
					currentVectorNearestLeaders.add(new IndexDistance(k, dist));
				}
				nearestLeaderIndex[j] = currentVectorNearestLeaders.peek().index;

				for(int k = 0; k < leadersToFollow; k++)
					followingLeaders[j][k] = currentVectorNearestLeaders.poll().index;
			}
			
			changed = false;
			//update leaders, check for change
			for(int k = 0; k < K; k++)
			{
				int numberOfFollowers = 0;
				tmpLeaderVector = new float[numberOfWords+1];
				for(int j = 1; j <= numberOfWords; j++)
					tmpLeaderVector[j] = 0;
				
				for(int j = 1; j <= numberOfDocs; j++)
				{
					if(nearestLeaderIndex[j] == k)
					{
						numberOfFollowers++;
						docVectorSize = vectors.getSize(j);
						docWords = vectors.getWords(j);
						docWeights = vectors.getWeights(j);
						
						for(int l = 0; l < docVectorSize; l++)
							tmpLeaderVector[docWords[l]] += docWeights[l];
					}
				}
			
				nonZeroElements = 0;
				for(int j = 1; j <= numberOfWords; j++)
					if(tmpLeaderVector[j] != 0)
						nonZeroElements++;
				
				oldVectorSize = leadersSize[k];
				oldVectorWords = new int[oldVectorSize];
				oldVectorWeights = new float[oldVectorSize];
				for(int j = 0; j < oldVectorSize; j++)
				{
					oldVectorWeights[j] = leadersWeights[k][j];
					oldVectorWords[j] = leadersWords[k][j];
				}
				
				leadersSize[k] = nonZeroElements;
				leadersWords[k] = new int[nonZeroElements];
				leadersWeights[k] = new float[nonZeroElements];
				
				int tmpIndex = 0;
				for(int j = 1; j <= numberOfWords; j++)
				{
					if(tmpLeaderVector[j] != 0)
					{
						leadersWords[k][tmpIndex] = j;
						leadersWeights[k][tmpIndex] = tmpLeaderVector[j]/numberOfFollowers;
					}
				}
				
				//check for change
				if(!changed)
				{
					if(oldVectorSize != leadersSize[k])
						changed = true;
					else
					{
						for(int j = 0; j < leadersSize[k]; j++)
						{
							if(leadersWords[k][j] != oldVectorWords[j] || leadersWeights[k][j] != oldVectorWeights[j])
							{
								changed = true;
								break;
							}
						}
					}
				}
			}
		}
		
		clusters = new int[K][];
		clusterSize = new int[K];
		for(int i = 1; i <= numberOfDocs; i++)
			for(int j = 0; j < leadersToFollow; j++)
				clusterSize[followingLeaders[i][j]]++;
		
		int[] lastIndex = new int[K];
		for(int i = 0; i < K; i++)
		{
			clusters[i] = new int[clusterSize[i]];
			lastIndex[i] = 0;
		}
		
		for(int i = 1; i <= numberOfDocs; i++)
		{
			for(int j = 0; j < leadersToFollow; j++)
			{
				clusters[followingLeaders[i][j]][lastIndex[followingLeaders[i][j]]] = i;
				lastIndex[followingLeaders[i][j]]++;
			}
		}
	}
	
	private boolean contains(int[] a, int size, int k)
	{
		for(int i = 0; i < size; i++)
			if(a[i] == k)
				return true;
		return false;
	}
	
	public void clear()
	{
		if(path != null)
			new File(path).delete();
	}

	@Override
	public int compare(IndexDistance o1, IndexDistance o2)
	{
		if(o1.distance > o2.distance)
			return -1;
		else if(o1.distance < o2.distance)
			return 1;
		else if(o1.distance == o2.distance)
			return 0;
		return 0;
	}
}
