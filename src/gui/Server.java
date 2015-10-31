package gui;

import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import clustering.IndexDistance;
import clustering.KMeans;
import dictionary.TrainTST;
import read.MyTokenizer;
import read.Normalizer;
import read.StopWords;
import read.TokenDocType;
import read.TrainDocWordIndex;
import stemmer.PaiceStemmer;
import vector.CosineCalculator;
import vector.TrainDocVectorCalculator;

public class Server extends JFrame implements ActionListener, PropertyChangeListener, WindowListener
{
	private static final long serialVersionUID = 1L;
	
	private static final int RESULT_SIZE = 10;

	Server thisServer = this;
	
	JFrame frame = null;
	JPanel mainPanel = null;
	JTextField searchField = null;
	Cursor searchFieldCursor = null;
	JButton processFileButton = null;
	JButton searchButton = null;
	JButton selectMainFile = null;
	JButton selectStemminRules = null;
	JTextArea resultArea = null;
	JScrollPane scrollPane = null;
	JFileChooser fileChooser = null;
	JProgressBar progressBar = null;
	Task task = null;
	boolean stemLoaded = false;
	boolean fileLoaded = false;
	String mainFilePath = null;
	String stemmingRulesPath = null;

	Runtime runtime = null;
	long startTime;
	MyTokenizer tokenizer = null;
	PaiceStemmer stemmer = null;
	StopWords stopword = null;
	TrainTST dictionary = null;
//	DocWordIndex docWordIndex = null;
	TrainDocWordIndex trainDocWordIndex = null;
//	DocVectorCalculator vectors = null;
	TrainDocVectorCalculator trainDocVectorCalculator = null;
	KMeans clusters = null;
	ArrayList<Long> docPositionInMainFile = null;
	
	static final int GAP = 10;
	static final int FIELD_WIDTH = 300;
	static final int BUTTON_HEIGHT = 25;
	static final int BUTTON_WIDTH = 100;
	static final int FRAME_HEIGHT = 500;
	static final int FRAME_WIDTH = 600;

	private static final int LEADERS_TO_SEARCH = 4;

	class Task extends SwingWorker<Void, Void> {
		/*
		 * Main task. Executed in background thread.
		 */
		@Override
		public Void doInBackground() throws IOException
		{
			tokenizer = new MyTokenizer(mainFilePath, "!@#$%^&*()_-=+\r \n?:{}|./,;\\'[]'\"<>", MyTokenizer.BUFF_SIZE);
			dictionary = new TrainTST();
			try {
				dictionary.setPath(mainFilePath.substring(0, mainFilePath.lastIndexOf("\\")));
			} catch (IOException e2) {
				e2.printStackTrace();
			}
			docPositionInMainFile = new ArrayList<Long>();
			docPositionInMainFile.add((long)-1); // first doc index is 1
			trainDocWordIndex = new TrainDocWordIndex();
			if(trainDocVectorCalculator != null)
				trainDocVectorCalculator.clear();
//			docWordIndex.reset();
//			if(vectors != null)
//				vectors.clear();
			
			startTime = System.currentTimeMillis();
			int word = 0;
			int progress = 0;
			int wordID = 0;
			setProgress(0);

			resultArea.append("Processing file...\n");
			resultArea.setCaretPosition(resultArea.getText().length());
			int lastDoc = -1;
			String s1, s2;
			while(tokenizer.hasMoreTokens())
			{
				word++;
				TokenDocType td = tokenizer.getNextToken();
				s1 = Normalizer.removeAccents(td.token).toLowerCase();
				if(s1.length() == 0)
				{
					if(lastDoc != td.doc)
					{
						lastDoc = td.doc;
						docPositionInMainFile.add(tokenizer.getLastDocPos());
					}
					continue;
				}
				s2 = new String(s1);
				if(!stopword.isStopWord(s1))
				{
					s2 = stemmer.stripAffixes(s1);
					if(s2.length() == 0)
						s2 = s1;
					wordID = dictionary.add(s2, td.doc);
					trainDocWordIndex.add(td.doc, wordID);
//					docWordIndex.add(td.doc, wordID);
				}

				if(lastDoc != td.doc)
				{
					lastDoc = td.doc;
					docPositionInMainFile.add(tokenizer.getLastDocPos());
				}
				
				if((int) ((long)tokenizer.buffNum * (long)MyTokenizer.BUFF_SIZE * (long)100 / tokenizer.fileSize) > progress)
				{
					progress = (int) ((long)tokenizer.buffNum * (long)MyTokenizer.BUFF_SIZE * (long)100 / tokenizer.fileSize);
					if(progress <= 100)
						setProgress(progress);
					else
						setProgress(100);
				}
			}

			trainDocWordIndex.finish();
//			docWordIndex.close();
			
			dictionary.clearPostingsLists();
			setProgress(100);
			resultArea.append("\n"+word+" wrods.\n");
			resultArea.append(dictionary.lastID+" distinct words (after stemming).\n");
			System.gc();
			long time = System.currentTimeMillis() - startTime;
			long allocatedMemory = runtime.totalMemory();
			long freeMemory = runtime.freeMemory();
			long usingMemory = allocatedMemory - freeMemory;
			usingMemory /= 1024*1024;
			resultArea.append("Memory usage: "+usingMemory+" MB.\n");
			resultArea.append("Process time: "+((double)time/1000.0)+" sec.\n");
			resultArea.append("Dictionary(TST) size: "+(dictionary.numberOfNodes)+" nodes.\n");
			resultArea.setCaretPosition(resultArea.getText().length());
			
			trainDocVectorCalculator = new TrainDocVectorCalculator(dictionary, trainDocWordIndex, trainDocWordIndex.getNumberOfDocs(),clusters);
//			vectors = new DocVectorCalculator("vectors", dictionary, trainDocWordIndex, trainDocWordIndex.getNumberOfDocs());
//			vectors = new DocVectorCalculator("vectors", dictionary, docWordIndex, docWordIndex.getTotalDoc());
//			vectors.setPath(mainFilePath.substring(0, mainFilePath.lastIndexOf("\\")));
			startTime = System.currentTimeMillis();
			resultArea.append("Creating vectors...\n");
			resultArea.setCaretPosition(resultArea.getText().length());
			vector.TrainDocVectorCalculator.Task task = trainDocVectorCalculator.new Task(startTime, resultArea, selectMainFile, selectStemminRules, processFileButton, searchButton, searchField, searchFieldCursor, frame, thisServer);
//			vector.DocVectorCalculator.Task task = vectors.new Task(startTime, resultArea, selectMainFile, selectStemminRules, processFileButton, searchButton, searchField, searchFieldCursor, frame);
			task.addPropertyChangeListener(thisServer);
			task.execute();
			
			try {
				tokenizer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		/*
		 * Executed in event dispatching thread
		 */
		@Override
		public void done()
		{
		}
	}	
	public Server() throws FileNotFoundException
	{
		runtime = Runtime.getRuntime();
		startTime = System.currentTimeMillis();

		stopword = new StopWords();
		dictionary = new TrainTST();
		trainDocWordIndex = new TrainDocWordIndex();
//		docWordIndex = new DocWordIndex("doc word index");
		docPositionInMainFile = new ArrayList<Long>();
		docPositionInMainFile.add((long)-1); // first doc index is 1

		Normalizer.initialize();

		//frame
		frame = this;
		frame.setTitle("IR Project Phase 3");
		frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
		frame.setVisible(true);
		frame.setLayout(null);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
		frame.setResizable(false);

		//panel
		mainPanel = new JPanel();
		mainPanel.setSize(FRAME_WIDTH, FRAME_HEIGHT);
		mainPanel.setVisible(true);
		mainPanel.setLayout(null);

		int newButtonWidth = (FIELD_WIDTH+GAP+BUTTON_WIDTH - 2*GAP)/3; 
		//process file button
		processFileButton = new JButton();
		processFileButton.setBounds((FRAME_WIDTH-(FIELD_WIDTH+GAP+BUTTON_WIDTH))/2+GAP+newButtonWidth+GAP+newButtonWidth, 2*GAP+BUTTON_HEIGHT+GAP+FRAME_HEIGHT-(2*GAP+BUTTON_HEIGHT+GAP+GAP+BUTTON_HEIGHT+GAP+BUTTON_HEIGHT+5*GAP)+GAP, newButtonWidth, BUTTON_HEIGHT);
		processFileButton.setVisible(true);
		processFileButton.setText("Process File");
		processFileButton.addActionListener(this);
		
		//select main file button
		selectMainFile = new JButton();
		selectMainFile.setBounds((FRAME_WIDTH-(FIELD_WIDTH+GAP+BUTTON_WIDTH))/2+GAP+newButtonWidth, 2*GAP+BUTTON_HEIGHT+GAP+FRAME_HEIGHT-(2*GAP+BUTTON_HEIGHT+GAP+GAP+BUTTON_HEIGHT+GAP+BUTTON_HEIGHT+5*GAP)+GAP, newButtonWidth, BUTTON_HEIGHT);
		selectMainFile.setVisible(true);
		selectMainFile.setText("Main file...");
		selectMainFile.addActionListener(this);
		
		//select stemming rules file button
		selectStemminRules = new JButton();
		selectStemminRules.setBounds((FRAME_WIDTH-(FIELD_WIDTH+GAP+BUTTON_WIDTH))/2, 2*GAP+BUTTON_HEIGHT+GAP+FRAME_HEIGHT-(2*GAP+BUTTON_HEIGHT+GAP+GAP+BUTTON_HEIGHT+GAP+BUTTON_HEIGHT+5*GAP)+GAP, newButtonWidth, BUTTON_HEIGHT);
		selectStemminRules.setVisible(true);
		selectStemminRules.setText("Stemming rules...");
		selectStemminRules.addActionListener(this);
		
		//search button
		searchButton = new JButton();
		searchButton.setBounds((FRAME_WIDTH-(FIELD_WIDTH+GAP+BUTTON_WIDTH))/2+FIELD_WIDTH+GAP, 2*GAP, BUTTON_WIDTH, BUTTON_HEIGHT);
		searchButton.setVisible(true);
		searchButton.setText("Search");
		searchButton.addActionListener(this);
		
		//search field
		searchField = new JTextField();
		searchField.setBounds((FRAME_WIDTH-(FIELD_WIDTH+GAP+BUTTON_WIDTH))/2, 2*GAP, FIELD_WIDTH, BUTTON_HEIGHT);
		searchField.setVisible(true);
		searchField.addActionListener(this);

		//result area
		resultArea = new JTextArea(18, 49);
		resultArea.setSize(FIELD_WIDTH+GAP+BUTTON_WIDTH, FRAME_HEIGHT-(2*GAP+BUTTON_HEIGHT+GAP));
		resultArea.setVisible(true);
		resultArea.setFont(new Font("Courier New Bold", Font.BOLD, 14));
		resultArea.setEditable(false);

		//scroll pane
		scrollPane = new JScrollPane(resultArea);
		scrollPane.setBounds((FRAME_WIDTH-(FIELD_WIDTH+GAP+BUTTON_WIDTH))/2, 2*GAP+BUTTON_HEIGHT+GAP, FIELD_WIDTH+GAP+BUTTON_WIDTH, FRAME_HEIGHT-(2*GAP+BUTTON_HEIGHT+GAP+GAP+BUTTON_HEIGHT+GAP+BUTTON_HEIGHT+5*GAP));
		scrollPane.setVisible(true);

		//progress bar
		progressBar = new JProgressBar(0, 100);
		progressBar.setSize(FIELD_WIDTH+GAP+BUTTON_WIDTH, BUTTON_HEIGHT);
		progressBar.setLocation((FRAME_WIDTH-(FIELD_WIDTH+GAP+BUTTON_WIDTH))/2 , 2*GAP+BUTTON_HEIGHT+GAP+FRAME_HEIGHT-(2*GAP+BUTTON_HEIGHT+GAP+GAP+BUTTON_HEIGHT+5*GAP)+GAP);
		progressBar.setValue(0);
		progressBar.setStringPainted(true);
		
		//file chooser
		javax.swing.filechooser.FileFilter fileFilter = new javax.swing.filechooser.FileFilter() {
			
			@Override
			public String getDescription()
			{
				return "Text";
			}
			
			@Override
			public boolean accept(File f)
			{
				if (f.isDirectory())
					return true;
				
				String extension = f.getName().substring(f.getName().lastIndexOf('.')+1).toLowerCase();
				if (extension != null)
				{
					if (extension.equals("dat") || extension.equals("txt"))
						return true;
				}
				else
					return false;
				
				return false;
			}
		};
		fileChooser = new JFileChooser();
		fileChooser.setFileFilter(fileFilter);
		
		
		mainPanel.add(selectMainFile);
		mainPanel.add(selectStemminRules);
		mainPanel.add(progressBar);
		mainPanel.add(searchButton);
		mainPanel.add(processFileButton);
		mainPanel.add(scrollPane);
		mainPanel.add(searchField);
		frame.add(mainPanel);
		frame.repaint();
		frame.addWindowListener(this);
		
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			public void run()
			{
				try {
					dictionary.shutdown();
				} catch (IOException e) {
					e.printStackTrace();
				}
//				try {
//					docWordIndex.clear();
//					if(vectors != null)
//						vectors.clear();	
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
			}
		});
	}

	public static void main(String[] args) throws FileNotFoundException
	{
		LookAndFeelSetter.setLookAndFeel();
		new Server();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if ("progress" == evt.getPropertyName())
		{
			int progress = (Integer) evt.getNewValue();
			progressBar.setValue(progress);
		} 

	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == searchField)
		{
			searchButton.doClick();
		}
		else if(e.getSource() == searchButton && searchField.getText().length() != 0)
		{
			if(!stemLoaded)
			{
				resultArea.append("Load stemming rules first.\n");
				resultArea.setCaretPosition(resultArea.getText().length());
				return;
			}
			
			if(!fileLoaded)
			{
				resultArea.append("Select main file first.\n");
				resultArea.setCaretPosition(resultArea.getText().length());
				return;
			}
			
			if(dictionary == null)
			{
				resultArea.append("Process main file first.\n");
				resultArea.setCaretPosition(resultArea.getText().length());
				return;
			}
			StringTokenizer searchTokenizer = new StringTokenizer(searchField.getText(), "!@#$%^&*()_-=+\r \n?:{}|./,;\\'[]'\"<>");
			ArrayList<String> queryTerms = new ArrayList<String>();
			
			if(!searchTokenizer.hasMoreTokens())
			{
				resultArea.append("Type a search query.");
				resultArea.setCaretPosition(resultArea.getText().length());
				return;
			}
			
			while(searchTokenizer.hasMoreTokens())
			{
				String s = searchTokenizer.nextToken();
				resultArea.append(s);
				if(searchTokenizer.hasMoreTokens())
					resultArea.append(" ");
				if(!stopword.isStopWord(s))
				{
					if(stemmer.stripAffixes(s).length() != 0)
						s = stemmer.stripAffixes(s);
					queryTerms.add(s);
				}
			}
			
			resultArea.append(":\n");

			if(queryTerms.size() == 0)
			{
				resultArea.append("Not found.\n");
				resultArea.setCaretPosition(resultArea.getText().length());
				return;
			}
					
			
			ArrayList<Integer> searchResult = new ArrayList<Integer>();
			int len = queryTerms.size();

			//create query vector
			float maxTF = 0;
			int[] queryWords = new int[len];
			float[] queryWeights = new float[len];
			for(int i = 0; i < len; i++)
				queryWeights[i] = queryWords[i] = 0;
			for(int i = 0; i < len; i++)
			{
				int id = 0;
				try {
					id = dictionary.getIDfromFile(queryTerms.get(i));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				boolean contains = false;
				int j;
				for(j = 0; j < i; j++)
					if(queryWords[j] == id)
					{
						contains = true;
						break;
					}
				
				if(contains)
				{
					queryWeights[j]++;
				}
				else
				{
					queryWords[i] = id;
					queryWeights[i]++;
				}
			}
			
			for(int i = 0; i < len; i++)
				if(queryWeights[i] > maxTF)
					maxTF = queryWeights[i];
				
			for(int i = 0; i < len-1; i++)
			{
				int minIndex = i;
				for(int j = i+1; j < len; j++)
					if(queryWords[minIndex] > queryWords[j])
						minIndex = j;
				
				int tmpi = queryWords[minIndex];
				float tmpf = queryWeights[minIndex];
				
				queryWords[minIndex] = queryWords[i];
				queryWeights[minIndex] = queryWeights[i];
				
				queryWords[i] = tmpi;
				queryWeights[i] = tmpf;
			}
			
			for(int i = 0; i < len; i++)
			{
				int df = 1;
				if(queryWeights[i] == 0 || queryWords[i] == 0) //doesn't add the terms that are not in dictionary
					continue;
				
				try {
					df = dictionary.getFrequencyFromFile(queryWords[i]);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				if(df == 0)
					df = 1;
				queryWeights[i] = queryWeights[i]/maxTF * ((float)Math.log10(trainDocWordIndex.getNumberOfDocs())-(float)Math.log10(df)); 
			}

			int[] finalQueryWord;
			float[] finalQueryWeights;
			int nonZeroElements = 0;
			for(int i = 0; i < len; i++)
				if(queryWeights[i] != 0)
					nonZeroElements++;
			
			finalQueryWeights = new float[nonZeroElements];
			finalQueryWord = new int[nonZeroElements];
			
			int cnt = 0;
			for(int i = 0; i < len; i++)
			{
				if(queryWeights[i] != 0)
				{
					finalQueryWeights[cnt] = queryWeights[i];
					finalQueryWord[cnt] = queryWords[i];
					cnt++;
				}
			}
			
			//search
			float dist;
			CosineCalculator cosine = new CosineCalculator();
			
			clusters = trainDocVectorCalculator.getClusters();
			PriorityQueue<IndexDistance> clustersMaxHeap = new PriorityQueue<IndexDistance>(clusters.K, new Comparator<IndexDistance>()
			{
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
			});
			
			PriorityQueue<IndexDistance> resultMaxHeap = new PriorityQueue<IndexDistance>(clusters.K, new Comparator<IndexDistance>()
			{
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
			});
					
			for(int i = 0; i < clusters.K; i++)
			{
				dist = cosine.cosineDistance(finalQueryWord, finalQueryWeights, nonZeroElements, clusters.leadersWords[i], clusters.leadersWeights[i], clusters.leadersSize[i]);
				clustersMaxHeap.add(new IndexDistance(i, dist));
			}
			
			int docID = 0;
			for(int j = 0; j < LEADERS_TO_SEARCH; j++)
			{
				int index = clustersMaxHeap.poll().index;
				for(int i = 0; i < clusters.clusterSize[index]; i++)
				{
					docID = clusters.clusters[index][i];
					dist = cosine.cosineDistance(finalQueryWord, finalQueryWeights, nonZeroElements, trainDocVectorCalculator.getWords(docID), trainDocVectorCalculator.getWeights(docID), trainDocVectorCalculator.getSize(docID));
					resultMaxHeap.add(new IndexDistance(docID, dist));
				}
			}
			for(int i = 0; i < RESULT_SIZE && !resultMaxHeap.isEmpty(); i++)
			{
				int resultIndex = resultMaxHeap.poll().index;
				if(!searchResult.contains(resultIndex))
					searchResult.add(resultIndex);
			}
			
			for(Integer i: searchResult)
			{
				resultArea.append(i+"\n");
			}
			if(searchResult.size() == 0)
				resultArea.append("Not found.");
			resultArea.append("\n\n");
			resultArea.setCaretPosition(resultArea.getText().length());
		}
		else if(e.getSource() == processFileButton)
		{
			if(!stemLoaded)
			{
				resultArea.append("Load stemming rules first.\n");
				resultArea.setCaretPosition(resultArea.getText().length());
				return;
			}
			
			if(!fileLoaded)
			{
				resultArea.append("Select main file first.\n");
				resultArea.setCaretPosition(resultArea.getText().length());
				return;
			}
	
			selectMainFile.setEnabled(false);
			selectStemminRules.setEnabled(false);
			processFileButton.setEnabled(false);
			searchButton.setEnabled(false);
			searchFieldCursor = searchField.getCursor();
			searchField.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			searchField.setEnabled(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			
			task = new Task();
			task.addPropertyChangeListener(this);
			task.execute();
		}
		else if(e.getSource() == selectMainFile)
		{
			if( fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION )
			{
				if(!fileChooser.getSelectedFile().exists())
				{
					resultArea.append("File \"" + fileChooser.getSelectedFile().getName() + "\" does not exist.\n");
					resultArea.setCaretPosition(resultArea.getText().length());				
				}
				else
				{
					mainFilePath = fileChooser.getSelectedFile().getAbsolutePath();
					
//					try {
//						docWordIndex.setPath(mainFilePath.substring(0, mainFilePath.lastIndexOf("\\")));
//					} catch (FileNotFoundException e2) {
//						e2.printStackTrace();
//					}
					try {
						tokenizer = new MyTokenizer(mainFilePath, "!@#$%^&*()_-=+\r \n?:{}|./,;\\'[]'\"<>", MyTokenizer.BUFF_SIZE);
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					}
					resultArea.append("Main file loaded:\n");
					resultArea.append("\t"+fileChooser.getSelectedFile().getName()+"\n\n");
					resultArea.setCaretPosition(resultArea.getText().length());
					fileLoaded = true;
				}
			}
		}
		else if(e.getSource() == selectStemminRules)
		{
			if( fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION )
			{
				if(!fileChooser.getSelectedFile().exists())
				{
					resultArea.append("File \"" + fileChooser.getSelectedFile().getName() + "\" does not exist.\n");
					resultArea.setCaretPosition(resultArea.getText().length());				
				}
				else
				{
					stemmingRulesPath = fileChooser.getSelectedFile().getAbsolutePath();
					stemmer = new PaiceStemmer(stemmingRulesPath, "");
					resultArea.append("Stemming rules loaded:\n");
					resultArea.append("\t"+fileChooser.getSelectedFile().getName()+"\n\n");
					resultArea.setCaretPosition(resultArea.getText().length());
					stemLoaded = true;
				}
			}
		}
	}

	@Override
	public void windowActivated(WindowEvent e)
	{
	}

	@Override
	public void windowClosed(WindowEvent e)
	{
		try {
			dictionary.shutdown();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
//		try {
//			docWordIndex.clear();
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
//		if(vectors != null)
//			try {
//				vectors.clear();
//			} catch (IOException e1) {
//				e1.printStackTrace();
//			}
	}

	@Override
	public void windowClosing(WindowEvent e)
	{
	}

	@Override
	public void windowDeactivated(WindowEvent e)
	{
	}

	@Override
	public void windowDeiconified(WindowEvent e)
	{
	}

	@Override
	public void windowIconified(WindowEvent e)
	{
	}

	@Override
	public void windowOpened(WindowEvent e)
	{
	}
}
