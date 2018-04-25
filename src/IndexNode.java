import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class IndexNode<K extends Comparable<K>, T> extends Node<K, T> {

	// m nodes
	private ArrayList<Node<K, T>> children; // m+1 children

	public IndexNode(K key, Node<K, T> child0, Node<K, T> child1, int degree) {
		super(degree);
		isLeafNode = false;
		addKey(key);
		children = new ArrayList<Node<K, T>>();
		children.add(child0);
		children.add(child1);
	}

	public IndexNode(List<K> newKeys, List<Node<K, T>> newChildren, int degree) {
		super(degree);
		isLeafNode = false;

		addAllKeys(newKeys);
		children = new ArrayList<Node<K, T>>(newChildren);

	}

	public ArrayList<Node<K, T>> getChildren() {
		return (ArrayList<Node<K, T>>) this.children.clone();
	}

	public Node<K, T> getChild(int i) {
		return children.get(i);
	}

	public int getNumChildren() {
		return this.children.size();
	}

	public Node<K, T> removeChild(int i) {
		return this.children.remove(i);
	}

	public void addChild(int i, Node<K, T> child) {
		this.children.add(i, child);
	}

	public void addChild(Node<K, T> child) {
		this.children.add(child);
	}

	/**
	 * insert the entry into this node at the specified index so that it still
	 * remains sorted
	 * 
	 * @param e
	 * @param index
	 */
	public void insertSorted(Entry<K, Node<K, T>> e, int index) {
		K key = e.getKey();
		Node<K, T> child = e.getValue();
		if (index >= getNumKeys()) {
			addKey(key);
			children.add(child);
		} else {
			addKey(index, key);
			children.add(index + 1, child);
		}
	}

	/**
	 * Split an indexNode and return the new right node and the splitting key as
	 * an Entry<slitingKey, RightNode>
	 * 
	 * @param index,
	 *            any other relevant data
	 * @return new key/node pair as an Entry
	 */
	public Entry<K, Node<K, T>> splitNode() {
		ArrayList<K> newKeys = new ArrayList<K>();
		ArrayList<Node<K, T>> newChildren = new ArrayList<Node<K, T>>();

		// Note difference with splitting leaf page, 2D+1 key values and 2D+2
		// node pointers
		K splitKey = this.getKey(this.degree);
		this.removeKey(this.degree);

		// First D key values and D+1 node pointers stay
		// Last D keys and D+1 pointers move to new node
		newChildren.add(this.removeChild(this.degree + 1));

		while (this.getNumKeys() > this.degree) {
			newKeys.add(this.getKey(this.degree));
			this.removeKey(this.degree);
			newChildren.add(this.removeChild(this.degree + 1));
		}

		IndexNode<K, T> rightNode = new IndexNode<K, T>(newKeys, newChildren, this.degree);
		Entry<K, Node<K, T>> newChildEntry = new AbstractMap.SimpleEntry<K, Node<K, T>>(splitKey, rightNode);

		return newChildEntry;
	}

	
	public Entry<K, Node<K, T>> deleteChildEntry(Node<K, T> parentNode, Entry<K, Node<K, T>> entry,
			Entry<K, Node<K, T>> oldChildEntry, Node<K, T> root) {
		// Choose subtree, find i such that Ki <= entry's key value < K(i+1)

		int i = 0;
		K entryKey = entry.getKey();
		while (i < this.getNumKeys()) {
			if (entryKey.compareTo(this.getKey(i)) < 0) {
				break;
			}
			i++;
		}
		// Recursive delete
		oldChildEntry = this.getChild(i).deleteChildEntry(this, entry, oldChildEntry, root);

		// Usual case: child not deleted
		if (oldChildEntry == null) {
			return null;
		}
		// Discarded child node case
		else {
			int j = 0;
			K oldKey = oldChildEntry.getKey();
			while (j < this.getNumKeys()) {
				if (oldKey.compareTo(this.getKey(j)) == 0) {
					break;
				}
				j++;
			}
			// Remove oldChildEntry from node
			this.removeKey(j);
			this.removeChild(j + 1);

			// Check for underflow, return null if empty
			if (!this.isUnderflowed() || this.getNumKeys() == 0) {
				// Node has entries to spare, delete doesn't go further
				return null;
			} else {
				// Return if root
				if (this == root) {
					return oldChildEntry;
				}
				// Get sibling S using parent pointer
				int s = 0;
				K firstKey = this.getKey(0);
				while (s < parentNode.getNumKeys()) {
					if (firstKey.compareTo(parentNode.getKey(s)) < 0) {
						break;
					}
					s++;
				}
				// Handle index underflow
				int splitKeyPos;
				IndexNode<K, T> parent = (IndexNode<K, T>) parentNode;

				if (s > 0 && parent.getChild(s - 1) != null) {
					splitKeyPos = handleIndexNodeUnderflow((IndexNode<K, T>) parent.getChild(s - 1), this, parent);
				} else {
					splitKeyPos = handleIndexNodeUnderflow(this, (IndexNode<K, T>) parent.getChild(s + 1), parent);
				}
				// S has extra entries, set oldChildentry to null, return
				if (splitKeyPos == -1) {
					return null;
				}
				// Merge indexNode and S
				else {
					K parentKey = parentNode.getKey(splitKeyPos);
					oldChildEntry = new AbstractMap.SimpleEntry<K, Node<K, T>>(parentKey, parentNode);
					return oldChildEntry;
				}
			}
		}
	}

	/**
	 * Handle IndexNode Underflow (merge or redistribution)
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
	public int handleIndexNodeUnderflow(IndexNode<K, T> leftIndex, IndexNode<K, T> rightIndex, IndexNode<K, T> parent) {
		// Find entry in parent for node on right
		int i = 0;
		K rKey = rightIndex.getKey(0);
		while (i < parent.getNumKeys()) {
			if (rKey.compareTo(parent.getKey(i)) < 0) {
				break;
			}
			i++;
		}
		// Redistribute evenly between node and S through parent
		// If S has extra entries
		if (leftIndex.getNumKeys() + rightIndex.getNumKeys() >= 2 * this.degree) {
			// Left node has more entries
			if (leftIndex.getNumKeys() > rightIndex.getNumKeys()) {
				while (leftIndex.getNumKeys() > this.degree) {
					rightIndex.addKey(0, parent.getKey(i - 1));
					rightIndex.addChild(leftIndex.removeChild(leftIndex.getNumChildren() - 1));
					parent.replaceKey(i - 1, leftIndex.removeKey(leftIndex.getNumKeys() - 1));
				}
			}
			// Right node has more entries
			else {
				while (leftIndex.getNumKeys() < this.degree) {
					leftIndex.addKey(parent.getKey(i - 1));
					leftIndex.addChild(rightIndex.removeChild(0));
					parent.replaceKey(i - 1, rightIndex.removeKey(0));
				}
			}
			return -1;
		}
		// No extra entries, return spiltKeyPos
		else {
			leftIndex.addKey(parent.getKey(i - 1));
			// Move all entries from right to left node
			while (rightIndex.getNumKeys() > 0) {
				leftIndex.addKey(rightIndex.removeKey(0));
				leftIndex.addChild(rightIndex.removeChild(0));
			}
			leftIndex.addChild(rightIndex.removeChild(0));
			return i - 1;
		}
	}

}
