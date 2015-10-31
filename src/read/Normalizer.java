package read;

import java.util.HashMap;

public abstract class Normalizer
{
	private static HashMap<Character, Character> MAP_NORM = null;
	
	public static void initialize() 
	{
		String abnormals = new String("ÂÃÄÀÁÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞß"
									+ "àáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ");
		if (MAP_NORM == null || MAP_NORM.size() == 0)
		{
			MAP_NORM = new HashMap<Character, Character>();
			
			for(int i = 0; i < abnormals.length(); i++)
			{
				if(i < 7)
					MAP_NORM.put(abnormals.charAt(i), 'a');
				else if(i < 8)
					MAP_NORM.put(abnormals.charAt(i), 'c');
				else if(i < 12)
					MAP_NORM.put(abnormals.charAt(i), 'e');
				else if(i < 16)
					MAP_NORM.put(abnormals.charAt(i), 'i');
				else if(i < 17)
					MAP_NORM.put(abnormals.charAt(i), 'd');
				else if(i < 18)
					MAP_NORM.put(abnormals.charAt(i), 'n');
				else if(i < 24)
					MAP_NORM.put(abnormals.charAt(i), 'o');
				else if(i < 28)
					MAP_NORM.put(abnormals.charAt(i), 'u');
				else if(i < 29)
					MAP_NORM.put(abnormals.charAt(i), 'y');
				else if(i < 31)
					MAP_NORM.put(abnormals.charAt(i), 'b');
				else if(i < 7 + 31)
					MAP_NORM.put(abnormals.charAt(i), 'a');
				else if(i < 8 + 31)
					MAP_NORM.put(abnormals.charAt(i), 'c');
				else if(i < 12 + 31)
					MAP_NORM.put(abnormals.charAt(i), 'e');
				else if(i < 16 + 31)
					MAP_NORM.put(abnormals.charAt(i), 'i');
				else if(i < 17 + 31)
					MAP_NORM.put(abnormals.charAt(i), 'd');
				else if(i < 18 + 31)
					MAP_NORM.put(abnormals.charAt(i), 'n');
				else if(i < 24 + 31)
					MAP_NORM.put(abnormals.charAt(i), 'o');
				else if(i < 28 + 31)
					MAP_NORM.put(abnormals.charAt(i), 'u');
				else if(i < 29 + 31)
					MAP_NORM.put(abnormals.charAt(i), 'y');
				else if(i < 30 + 31)
					MAP_NORM.put(abnormals.charAt(i), 'b');
				else if(i < 31 + 31)
					MAP_NORM.put(abnormals.charAt(i), 'y');
			}
		}
	}
	
	public static String removeAccents(String value)
	{
		StringBuilder sb = new StringBuilder(value);

		for(int i = 0; i < value.length(); i++) {
			Character c = MAP_NORM.get(sb.charAt(i));
			if(c != null)
			{
				sb.setCharAt(i, c.charValue());
			}
			else if(c == null && notValid(sb.charAt(i)))
			{
				sb.setCharAt(i, 'e');
			}
		}
		return sb.toString();

	}

	private static boolean notValid(char c)
	{
		if((c >= 'A' && c <= 'Z') || 
		   (c >= 'a' && c <= 'z') ||
		   (c >= '0' && c <= '9'))
			return false;
		
		return true;
	}
}
