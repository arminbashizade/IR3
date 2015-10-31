package vector;

import java.util.ArrayList;

public class CosineCalculator
{
	
	public CosineCalculator()
	{
	}

	public float cosineDistance(int[] v1words, float[] v1weights, int v1size,
								int[] v2words, float[] v2weights, int v2size)
	{
		float result = 0, l1 = 0, l2 = 0;
		int p1, p2;
		p1 = 0;
		p2 = 0;
		
		int word1 = 0, word2 = 0;
		float w1 = 0, w2 = 0;
		
		while(p1 < v1size && p2 < v2size)
		{
			word1 = v1words[p1];
			word2 = v2words[p2];
			
			if(word1 > word2)
				p2++;
			else if(word1 < word2)
				p1++;
			else
			{
				w1 = v1weights[p1];
				w2 = v2weights[p2];
				
				result += w1*w2;
				p1++;
				p2++;
			}
		}

		while(p2 < v2size)
		{
			word2 = v2words[p2];
			if(word1 == word2)
			{
				w2 = v2weights[p2];
				result += w1*w2;
			}
			p2++;
		}
		
		while(p1 < v1size)
		{
			word1 = v1words[p1];
			if(word1 == word2)
			{
				w1 = v1weights[p1];
				result += w1*w2;
			}			
			p1++;
		}
		
		l1 = 0;
		for(int i = 0; i < v1size; i++)
		{
			w1 = v1weights[i];
			l1 += w1*w1;
		}
		
		l2 = 0;
		for(int i = 0; i < v2size; i++)
		{
			w2 = v2weights[i];
			l2 += w2*w2;
		}
		
		l1 = (float) Math.sqrt(l1);
		l2 = (float) Math.sqrt(l2);
		
		if(l1*l2 == 0)
			return -1;
		result /= l1*l2;
		
		return result;
	}
	
	public float cosineDistance(DocVector v1, DocVector v2)
	{
		float result = 0, l1 = 0, l2 = 0;
		int p1, p2;
		p1 = 0;
		p2 = 0;
		
		int word1, word2;
		float w1 = 0, w2 = 0;
		
		int size1, size2;
		size1 = v1.word.size();
		size2 = v2.word.size();
		
		do
		{
			word1 = v1.word.get(p1);
			word2 = v2.word.get(p2);
			
			if(word1 > word2)
				p2++;
			else if(word1 < word2)
				p1++;
			else
			{
				w1 = v1.weight.get(p1);
				w2 = v2.weight.get(p2);
				
				result += w1*w2;
				p1++;
				p2++;
			}
		}while(p1 < size1 && p2 < size2);

		while(p2 < size2)
		{
			word2 = v2.word.get(p2);
			if(word1 == word2)
			{
				w2 = v2.weight.get(p2);
				
				result += w1*w2;
			}
			p2++;
		}
		
		while(p1 < size1)
		{
			word1 = v1.word.get(p1);
			if(word1 == word2)
			{
				w1 = v1.weight.get(p1);
				
				result += w1*w2;
			}			
			p1++;
		}
		
		l1 = 0;
		for(int i = 0; i < size1; i++)
		{
			w1 = v1.weight.get(i);
			l1 += w1*w1;
		}
		
		l2 = 0;
		for(int i = 0; i < size2; i++)
		{
			w2 = v2.weight.get(i);
			l2 += w2*w2;
		}
		
		l1 = (float) Math.sqrt(l1);
		l2 = (float) Math.sqrt(l2);
		
		if(l1*l2 == 0)
			return -1;
		result /= l1*l2;
		
		return result;
	}
	
	public float getLength(int[] v1words, float[] v1weights, int v1size)
	{
		float l1 = 0;
		for(int i = 0; i < v1size; i++)
		{
			float w1 = v1weights[i];
			l1 += w1*w1;
		}
		return (float) Math.sqrt(l1);
	}
	
	public static void main(String[] args)
	{
		DocVector v1, v2;
		ArrayList<Integer> words = new ArrayList<Integer>();
		ArrayList<Float> weights = new ArrayList<Float>();
		
		words.add(1);
		words.add(2);
		words.add(3);
		words.add(4);
		
		weights.add((float) 1.0);
		weights.add((float) 1.0);
		weights.add((float) 1.0);
		weights.add((float) 1.0);
		
		v1 = new DocVector(words, weights);
		
		words.clear();
		words.add(1);
		words.add(2);
		words.add(3);
		
		weights.clear();
		weights.add((float) 1.0);
		weights.add((float) 1.0);
		weights.add((float) 1.0);
	
		v2 = new DocVector(words, weights);
		
		CosineCalculator calc = new CosineCalculator();
		
		System.out.println(calc.cosineDistance(v1, v2));
		
		int[] wordsarray = {1, 2, 3, 5};
		float[] weightsarray = {1, 1, 1, 1};
		int[] wordsarray1 = {1, 2, 4, 5};
		float[] weightsarray1 = {1, 1, 1, 1};
		System.out.println(calc.cosineDistance(wordsarray, weightsarray, 4, wordsarray1, weightsarray1, 3));
	}
}
