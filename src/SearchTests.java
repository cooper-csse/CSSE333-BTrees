import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Test;

public class SearchTests {

	private BPlusTree<Long, String> tree;
	private int degree = 8;
	
	@Before
	public void loadData() {
		tree = new BPlusTree<Long, String>(degree);
		Scanner s=null;
		try {
			s = new Scanner(new File(System.getProperty("user.dir") + "/bin/largeCase1.txt"));
			while (s.hasNextLong()) {
				long key = s.nextLong();
				if (s.hasNext()) {
					String value = s.next();
					tree.insert(key, value);
				}
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		finally {
			if (s!=null) {
				s.close();
			}
		}
	}
	
	@Test
	public void findSingleItem() {
		long key = 763485;
		String value = "763485Data";
		String result = tree.exactMatchSearch(key);
		
		assertEquals(value, result);
	}
	
	@Test
	public void findSingleItem2() {
		long key = 930204;
		String value = "930204Data";
		String result = tree.exactMatchSearch(key);
		assertEquals(value, result);
	}
	
	@Test
	public void itemNotFound() {
		long key = 930205;
		String value = null;
		String result = tree.exactMatchSearch(key);
		assertEquals(value, result);
	}
	
	@Test 
	public void findRangeGreaterThanEqualToKey() {
		long key = 930204;
		String[] expecteds = {"930204Data", "939488Data", "942874Data", "953121Data", "995513Data"};
		List<String> actuals = tree.greaterThanEqualToKeySearch(key);
		String[] actualsArray = new String[actuals.size()];
		actuals.toArray(actualsArray);
		assertArrayEquals(expecteds, actualsArray);
	}

	@Test 
	public void findRangeGreaterThanEqualToWithNonExistentKey() {
		long key = 930203;
		String[] expecteds = {"930204Data", "939488Data", "942874Data", "953121Data", "995513Data"};
		List<String> actuals = tree.greaterThanEqualToKeySearch(key);
		String[] actualsArray = new String[actuals.size()];
		actuals.toArray(actualsArray);
		assertArrayEquals(expecteds, actualsArray);
	}
	
	@Test 
	public void findRangeLessThanEqualToKey() {
		long key = 71513;
		String[] expecteds = {"19105Data", "21543Data", "22899Data", "23178Data", "36131Data", "46888Data", "63489Data", "71513Data"};
		List<String> actuals = tree.lessThanEqualToKeySearch(key);
		String[] actualsArray = new String[actuals.size()];
		actuals.toArray(actualsArray);
		assertArrayEquals(expecteds, actualsArray);
	}
	
	@Test 
	public void findRangeLessThanEqualToNonExistentKey() {
		long key = 71514;
		String[] expecteds = {"19105Data", "21543Data", "22899Data", "23178Data", "36131Data", "46888Data", "63489Data", "71513Data"};
		List<String> actuals = tree.lessThanEqualToKeySearch(key);
		String[] actualsArray = new String[actuals.size()];
		actuals.toArray(actualsArray);
		assertArrayEquals(expecteds, actualsArray);
	}
}
