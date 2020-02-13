import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

public class LeafNode<K extends Comparable<K>, T> extends Node<K, T> {
	private ArrayList<T> values;
	private LeafNode<K, T> nextLeaf;
	private LeafNode<K, T> previousLeaf;

	public LeafNode(K firstKey, T firstValue, int degree) {
		super(degree);
		isLeafNode = true;
		values = new ArrayList<T>();
		this.addKey(firstKey);
		values.add(firstValue);

	}

	public LeafNode(List<K> newKeys, List<T> newValues, int degree) {
		super(degree);
		isLeafNode = true;
		addAllKeys(newKeys);
		values = new ArrayList<T>(newValues);

	}

	/**
	 * insert key/value into this node so that it still remains sorted
	 * 
	 * @param key
	 * @param value
	 */
	public void insertSorted(K key, T value) {
		if (key.compareTo(getKey(0)) < 0) {
			addKey(0, key);
			values.add(0, value);
		} else if (key.compareTo(getKey(getNumKeys() - 1)) > 0) {
			addKey(key);
			values.add(value);
		} else {
			ListIterator<K> iterator = getKeyIterator();
			while (iterator.hasNext()) {
				if (iterator.next().compareTo(key) > 0) {
					int position = iterator.previousIndex();
					addKey(position, key);
					values.add(position, value);
					break;
				}
			}

		}
	}

	public T findValue(K key) {
		int index = this.findValueIndex(key);
		if (index == -1) return null;
		return this.getValue(index);
	}
	
	public int findValueIndex(K key) {
		if (this.getNumKeys() == 0) return -1;
		return this.binarySearch(key, 0, this.getNumKeys() - 1);
	}

	public int binarySearch(K key, int left, int right) {
		if (left > right) return -1;
		int mid = (left + right) / 2;
		int diff = this.getKey(mid).compareTo(key);
		if (diff == 0) return mid;
		else if (diff < 0) return binarySearch(key, mid + 1, right);
		return binarySearch(key, left, mid - 1);
	}

	public int getNumValues() {
		return this.values.size();
	}

	public T getValue(int i) {
		return this.values.get(i);
	}

	public T removeValue(int i) {
		return this.values.remove(i);
	}

	public void addValue(int i, T value) {
		this.values.add(i, value);
	}

	public void addValue(T value) {
		this.values.add(value);
	}

	public void addAllValues(List<T> values) {
		this.values.addAll(values);
	}

	public ListIterator<T> getValueIterator() {
		return this.values.listIterator();
	}

	public void replaceValue(int i, T value) {
		this.values.set(i, value);
	}

	public LeafNode<K, T> getNextLeaf() {
		return this.nextLeaf;
	}

	public LeafNode<K, T> getPreviousLeaf() {
		return this.previousLeaf;
	}

	public void setPreviousLeaf(LeafNode<K, T> node) {
		this.previousLeaf = node;

	}

	public void setNextLeaf(LeafNode<K, T> node) {
		this.nextLeaf = node;
	}

	/**
	 * Split a leaf node and return the new right node and the splitting key as
	 * an Entry<slitingKey, RightNode>
	 * 
	 * @param leaf,
	 *            any other relevant data
	 * @return the key/node pair as an Entry
	 */
	public Entry<K, Node<K, T>> splitNode() {
		ArrayList<K> newKeys = new ArrayList<K>();
		ArrayList<T> newValues = new ArrayList<T>();

		// The rest D entries move to brand new node
		for (int i = this.degree; i <= 2 * this.degree; i++) {
			newKeys.add(this.getKey(i));
			newValues.add(this.getValue(i));
		}

		// First D entries stay
		for (int i = this.degree; i <= 2 * this.degree; i++) {
			this.removeKey(this.getNumKeys() - 1);
			this.removeValue(this.getNumValues() - 1);
		}

		K splitKey = newKeys.get(0);
		LeafNode<K, T> rightNode = new LeafNode<K, T>(newKeys, newValues, this.degree);

		// Set sibling pointers
		LeafNode<K, T> tmp = this.getNextLeaf();
		this.setNextLeaf(rightNode);
		this.getNextLeaf().setPreviousLeaf(rightNode);
		rightNode.setPreviousLeaf(this);
		rightNode.setNextLeaf(tmp);

		Entry<K, Node<K, T>> newChildEntry = new AbstractMap.SimpleEntry<K, Node<K, T>>(splitKey, rightNode);

		return newChildEntry;
	}

	@Override
	public Entry<K, Node<K, T>> deleteChildEntry(Node<K, T> parentNode, Entry<K, Node<K, T>> entry,
			Entry<K, Node<K, T>> oldChildEntry, Node<K, T> root) {
		// Look for value to delete
		for (int i = 0; i < this.getNumKeys(); i++) {
			if (this.getKey(i) == entry.getKey()) {
				this.removeKey(i);
				this.removeValue(i);
				break;
			}
		}
		// Usual case: no underflow
		if (!this.isUnderflowed()) {
			return null;
		}
		// Once in a while, the leaf becomes underflow
		else {
			// Return if root
			if (this == root || this.getNumKeys() == 0) {
				return oldChildEntry;
			}
			// Handle leaf underflow
			int splitKeyPos;
			K firstKey = this.getKey(0);
			K parentKey = parentNode.getKey(0);

			if (this.getPreviousLeaf() != null && firstKey.compareTo(parentKey) >= 0) {
				splitKeyPos = handleLeafNodeUnderflow(this.getPreviousLeaf(), this, (IndexNode<K, T>) parentNode);
			} else {
				splitKeyPos = handleLeafNodeUnderflow(this, this.getNextLeaf(), (IndexNode<K, T>) parentNode);
			}
			// S has extra entries, set oldChildEntry to null, return
			if (splitKeyPos == -1) {
				return null;
			}
			// Merge leaf and S
			else {
				parentKey = parentNode.getKey(splitKeyPos);
				oldChildEntry = new AbstractMap.SimpleEntry<K, Node<K, T>>(parentKey, parentNode);
				return oldChildEntry;
			}
		}
	}

	/**
	 * Handle LeafNode Underflow (merge or redistribution)
	 * 
	 * @param left
	 *            : the smaller node
	 * @param right
	 *            : the bigger node
	 * @param parent
	 *            : their parent index node
	 * @return the splitkey position in parent if merged so that parent can
	 *         delete the splitkey later on. -1 otherwise
	 */
	private int handleLeafNodeUnderflow(LeafNode<K, T> left, LeafNode<K, T> right, IndexNode<K, T> parent) {
		// Find entry in parent for node on right
		int i = 0;
		K rKey = right.getKey(0);
		while (i < parent.getNumKeys()) {
			if (rKey.compareTo(parent.getKey(i)) < 0) {
				break;
			}
			i++;
		}
		// Redistribute evenly between right and left nodes
		// If S has extra entries
		if (left.getNumKeys() + right.getNumKeys() >= 2 * this.degree) {
			// Left node has more entries
			if (left.getNumKeys() > right.getNumKeys()) {
				while (left.getNumKeys() > this.degree) {
					right.addKey(0, left.removeKey(left.getNumKeys() - 1));
					right.addValue(0, left.removeValue(left.getNumValues() - 1));
				}
			}
			// Right node has more entries
			else {
				while (left.getNumKeys() < this.degree) {
					left.addKey(right.removeKey(0));
					left.addValue(right.removeValue(0));
				}
			}
			// Replace key value in parent entry by low-key in right node
			parent.replaceKey(i - 1, right.getKey(0));

			return -1;
		}
		// No extra entries, return splitKeyPos
		else {
			// Move all entries from right to left node
			while (right.getNumKeys() > 0) {
				left.addKey(right.removeKey(0));
				left.addValue(right.removeValue(0));
			}
			// Adjust sibling pointers
			if (right.getNextLeaf() != null) {
				right.getNextLeaf().setPreviousLeaf(left);
			}
			left.setNextLeaf(right.getNextLeaf());

			return i - 1;
		}
	}

}
