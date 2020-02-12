import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

public abstract class Node<K extends Comparable<K>, T> {
	protected boolean isLeafNode;
	private ArrayList<K> keys;
	protected int degree;

	public Node(int degree) {
		this.degree = degree;
		keys = new ArrayList<K>();
	}

	public boolean isOverflowed() {
		return keys.size() > 2 * degree;
	}

	public boolean isUnderflowed() {
		return keys.size() < degree;
	}

	public int getNumKeys() {
		return keys.size();
	}
	
	public K getKey(int i) {
		return this.keys.get(i);
	}
	
	public K removeKey(int i) {
		return this.keys.remove(i);
	}

	public boolean hasKey(K key) {
		return this.keys.contains(key);
	}

	public int getKeyIndex(K key) {
		return this.keys.indexOf(key);
	}
	
	public void addKey(int i, K key) {
		this.keys.add(i, key);
	}

	public void addKey(K key) {
		this.keys.add(key);
	}

	public void addAllKeys(List<K> keys) {
		this.keys.addAll(keys);
	}

	public ListIterator<K> getKeyIterator() {
		return this.keys.listIterator();
	}
	
	public void replaceKey(int i, K key) {
		this.keys.set(i, key);
	}
	
	
	public abstract Entry<K, Node<K,T>> deleteChildEntry(Node<K,T> parentNode, Entry<K, Node<K,T>> entry, 
			Entry<K, Node<K,T>> oldChildEntry, Node<K,T> root);

}
