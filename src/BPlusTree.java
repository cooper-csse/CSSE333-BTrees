import java.util.AbstractMap;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.List;

/**
 *  Code originally came from Cornell University students for a databases course. 
 *  Code was edited by Amanda Stouder for style, testing, and homework problem changes.
 * 
 */
public class BPlusTree<K extends Comparable<K>, T> {

	private Node<K, T> root;
	private int degree = 2;

	public BPlusTree(int degree) {
		this.degree = degree;
	}

	public Node<K, T> getRoot() {
		return root;
	}

	public int getDegree() {
		return this.degree;
	}

	/**
	 * Search the value for a specific key, and return the data at that key. You
	 * can think of the "Key" as the index field, and the data as the data
	 * pointer or row data that it returns (depending on the type of index).
	 * 
	 * This search should do an exact match search. If the key is found in the
	 * tree, it should return the value of that item. If not, it should return a
	 * null value.
	 * 
	 * @param key
	 *            - The key to search for.
	 * @return value - The value of the item found. If key not found, null.
	 */
	public T exactMatchSearch(K key) {
		if (key == null) return null;
		Node<K, T> node = this.findLeafNode(this.getRoot(), key);
		LeafNode<K, T> leaf = (LeafNode<K, T>) node;
		if (leaf == null) return null;
		return leaf.findValue(key);
	}

	/**
	 * Finds the leaf node where the given key should exist. Do NOT find the
	 * value itself here, just the leaf node where the value should exist. This
	 * is a helper method for the exactMatchSearch, as well as the
	 * greaterThanEqualToKeySearch and lessThanEqualToKeySearch.
	 * 
	 * Implement this method first. When implemented correctly, the delete tests
	 * will work, However, all tests with "hybrid" in the name and those and
	 * contained in "SearchTests" will still fail.
	 * 
	 * 
	 * 
	 * @param startingNode
	 * @param key
	 * @return
	 */
	private Node<K, T> findLeafNode(Node<K, T> startingNode, K key) {
		Node<K, T> node = startingNode;
		while (node != null && !node.isLeafNode) {
			IndexNode<K, T> indexNode = (IndexNode<K, T>) node;
			int i = search(node, key);
			node = indexNode.getChild(i);
		}
		return node;
	}

	private int search(Node<K, T> node, K key) {
		if (node.getNumKeys() == 1) {
			if (node.getKey(0).compareTo(key) > 0) return 0;
			return 1;
		}
		for (int i = 0; i < node.getNumKeys(); i++) {
			if (i == node.getNumKeys() - 1) {
				if (node.getKey(i).compareTo(key) > 0) return i;
				return i + 1;
			}
			int l = node.getKey(i).compareTo(key), r = node.getKey(i + 1).compareTo(key);
			if (l > 0 && r > 0) return i;
			if (l == 0 || l > 0 && r < 0) return i + 1;
		}
		return -1;
	}



	/**
	 * Performs a greater than or equal to range search on the B+ tree.
	 * 
	 * If the given key is found in the tree, it returns a list of values with
	 * that key value or greater. If the value is not found, it returns a list
	 * of all items with keys greater than the given key value. Returned items
	 * must be in ascending order by their key field.
	 * 
	 * @param key
	 *            - The value to perform a >= search against.
	 * @return The values of the items that had a key >= to what was given.
	 */
	public List<T> greaterThanEqualToKeySearch(K key) {
		ArrayList<T> values = new ArrayList<>();
		LeafNode<K, T> node = (LeafNode<K, T>) this.findLeafNode(this.getRoot(), key);
		for (ListIterator<K> iterator = node.getKeyIterator(); iterator.hasNext();) {
			K k = iterator.next();
			if (k.compareTo(key) >= 0) values.add(node.findValue(k));
		}
		while (node.getNextLeaf() != null) {
			node = node.getNextLeaf();
			for (ListIterator<T> iterator = node.getValueIterator(); iterator.hasNext();) {
				values.add(iterator.next());
			}
		}
		return values;
	}

	/**
	 * Performs a greater than or equal to range search on the B+ tree.
	 * 
	 * If the given key is found in the tree, it returns a list of values with
	 * that key value or less. If the value is not found, it returns a list of
	 * all items with keys less than the given key value. Returned items must be
	 * in **ascending** order by their key field.
	 * 
	 * @param key
	 *            - The value to perform a <= search against.
	 * @return The values of the items that had a key <= to what was given.
	 */
	public List<T> lessThanEqualToKeySearch(K key) {
		ArrayList<T> values = new ArrayList<>();
		LeafNode<K, T> node = (LeafNode<K, T>) this.findLeafNode(this.getRoot(), key);
		for (ListIterator<K> iterator = node.getKeyIterator(); iterator.hasNext();) {
			K k = iterator.next();
			if (k.compareTo(key) <= 0) values.add(node.findValue(k));
		}
		while (node.getPreviousLeaf() != null) {
			node = node.getPreviousLeaf();
			for (ListIterator<T> iterator = node.getValueIterator(); iterator.hasNext();) {
				values.add(iterator.next());
			}
		}
		return values;
	}

	/**
	 * Insert a key/value pair into the BPlusTree
	 * 
	 * @param key
	 * @param value
	 */
	public void insert(K key, T value) {
		LeafNode<K, T> newLeaf = new LeafNode<K, T>(key, value, this.degree);
		Entry<K, Node<K, T>> entry = new AbstractMap.SimpleEntry<K, Node<K, T>>(key, newLeaf);

		// Insert entry into subtree with root node pointer
		if (root == null || root.getNumKeys() == 0) {
			root = entry.getValue();
		}

		// newChildEntry null initially, and null on return unless child is
		// split
		Entry<K, Node<K, T>> newChildEntry = getChildEntry(root, entry, null);

		if (newChildEntry == null) {
			return;
		} else {
			IndexNode<K, T> newRoot = new IndexNode<K, T>(newChildEntry.getKey(), root, newChildEntry.getValue(),
					this.degree);
			root = newRoot;
			return;
		}
	}

	private Entry<K, Node<K, T>> getChildEntry(Node<K, T> node, Entry<K, Node<K, T>> entry,
			Entry<K, Node<K, T>> newChildEntry) {
		if (!node.isLeafNode) {
			// Choose subtree, find i such that Ki <= entry's key value < J(i+1)
			IndexNode<K, T> index = (IndexNode<K, T>) node;
			int i = 0;
			while (i < index.getNumKeys()) {
				if (entry.getKey().compareTo(index.getKey(i)) < 0) {
					break;
				}
				i++;
			}
			// Recursively, insert entry
			newChildEntry = getChildEntry((Node<K, T>) index.getChild(i), entry, newChildEntry);

			// Usual case, didn't split child
			if (newChildEntry == null) {
				return null;
			}
			// Split child case, must insert newChildEntry in node
			else {
				int j = 0;
				while (j < index.getNumKeys()) {
					if (newChildEntry.getKey().compareTo(index.getKey(j)) < 0) {
						break;
					}
					j++;
				}

				index.insertSorted(newChildEntry, j);

				// Usual case, put newChildEntry on it, set newChildEntry to
				// null, return
				if (!index.isOverflowed()) {
					return null;
				} else {
					newChildEntry = index.splitNode();

					// Root was just split
					if (index == root) {
						// Create new node and make tree's root-node pointer
						// point to newRoot
						IndexNode<K, T> newRoot = new IndexNode<K, T>(newChildEntry.getKey(), root,
								newChildEntry.getValue(), this.degree);
						root = newRoot;
						return null;
					}
					return newChildEntry;
				}
			}
		}
		// Node pointer is a leaf node
		else {
			LeafNode<K, T> leaf = (LeafNode<K, T>) node;
			LeafNode<K, T> newLeaf = (LeafNode<K, T>) entry.getValue();

			leaf.insertSorted(entry.getKey(), newLeaf.getValue(0));

			// Usual case: leaf has space, put entry and set newChildEntry to
			// null and return
			if (!leaf.isOverflowed()) {
				return null;
			}
			// Once in a while, the leaf is full
			else {
				newChildEntry = leaf.splitNode();
				if (leaf == root) {
					IndexNode<K, T> newRoot = new IndexNode<K, T>(newChildEntry.getKey(), leaf,
							newChildEntry.getValue(), this.degree);
					root = newRoot;
					return null;
				}
				return newChildEntry;
			}
		}
	}

	/**
	 * Delete a key/value pair from this B+Tree
	 * 
	 * @param key
	 */
	public void delete(K key) {
		if (key == null || root == null) {
			return;
		}

		// Check if entry key exist in the leaf node
		LeafNode<K, T> leaf = (LeafNode<K, T>) findLeafNode(root, key);
		if (leaf == null) {
			return;
		}

		// Delete entry from subtree with root node pointer
		Entry<K, Node<K, T>> entry = new AbstractMap.SimpleEntry<K, Node<K, T>>(key, leaf);

		// oldChildEntry null initially, and null upon return unless child
		// deleted
		Entry<K, Node<K, T>> oldChildEntry = root.deleteChildEntry(root, entry, null, root);

		// Readjust the root, no child is deleted
		if (oldChildEntry == null) {
			if (root.getNumKeys() == 0) {
				if (!root.isLeafNode) {
					root = ((IndexNode<K, T>) root).getChild(0);
				}
			}
			return;
		}
		// Child is deleted
		else {
			// Find empty node
			int i = 0;
			K oldKey = oldChildEntry.getKey();
			while (i < root.getNumKeys()) {
				if (oldKey.compareTo(root.getKey(i)) == 0) {
					break;
				}
				i++;
			}
			// Return if empty node already discarded
			if (i == root.getNumKeys()) {
				return;
			}
			// Discard empty node
			root.removeKey(i);
			((IndexNode<K, T>) root).removeChild(i + 1);
			return;
		}
	}
}