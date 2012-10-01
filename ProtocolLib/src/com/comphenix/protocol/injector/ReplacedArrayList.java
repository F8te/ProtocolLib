package com.comphenix.protocol.injector;

import java.util.Collection;
import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.BiMap;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.HashBiMap;

/**
 * Represents an array list that wraps another list, while automatically replacing one element with another.
 * <p>
 * The replaced elements can be recovered. 
 * 
 * @author Kristian
 * @param <TKey> - type of the elements we're replacing.
 */
class ReplacedArrayList<TKey> extends ForwardingList<TKey> {
	private BiMap<TKey, TKey> replaceMap = HashBiMap.create();
	private List<TKey> underlyingList;
	
	public ReplacedArrayList(List<TKey> underlyingList) {
		this.underlyingList = underlyingList;
	}
	
	@Override
	public boolean add(TKey element) {
		if (replaceMap.containsKey(element)) {
			return super.add(replaceMap.get(element));
		} else {
			return super.add(element);
		}
	}
	
	@Override
	public void add(int index, TKey element) {
		if (replaceMap.containsKey(element)) {
			super.add(index, replaceMap.get(element));
		} else {
			super.add(index, element);
		}
	}
	
	@Override
	public boolean addAll(Collection<? extends TKey> collection) {
		int oldSize = size();
		
		for (TKey element : collection)
			add(element);
		return size() != oldSize;
	}
	
	@Override
	public boolean addAll(int index, Collection<? extends TKey> elements) {
		int oldSize = size();
		
		for (TKey element : elements)
			add(index++, element);
		return size() != oldSize;
	}
	
	@Override
	protected List<TKey> delegate() {
		return underlyingList;
	}

	/**
	 * Add a replace rule.
	 * <p>
	 * This automatically replaces every existing element.
	 * @param target - instance to find.
	 * @param replacement - instance to replace with.
	 */
	public synchronized void addMapping(TKey target, TKey replacement) {
		replaceMap.put(target, replacement);

		// Replace existing elements
		replaceAll(target, replacement);
	}
	
	/**
	 * Revert the given mapping.
	 * @param target - the instance we replaced.
	 */
	public synchronized void removeMapping(TKey target) {
		// Make sure the mapping exist
		if (replaceMap.containsKey(target)) {
			TKey replacement = replaceMap.get(target);
			replaceMap.remove(target);
	
			// Revert existing elements
			replaceAll(replacement, target);
		}
	}
	
	/**
	 * Replace all instances of the given object.
	 * @param find - object to find.
	 * @param replace - object to replace it with.
	 */
	public synchronized void replaceAll(TKey find, TKey replace) {
		for (int i = 0; i < underlyingList.size(); i++) {
			if (Objects.equal(underlyingList.get(i), find))
				underlyingList.set(i, replace);
		}
	}
	
	/**
	 * Undo all replacements.
	 */
	public synchronized void revertAll() {
		
		// No need to do anything else
		if (replaceMap.size() < 1)
			return;
		
		BiMap<TKey, TKey> inverse = replaceMap.inverse();
		
		for (int i = 0; i < underlyingList.size(); i++) {
			TKey replaced = underlyingList.get(i);
			
			if (inverse.containsKey(replaced)) {
				underlyingList.set(i, inverse.get(replaced));
			}
		}
		
		replaceMap.clear();
	}
	
	@Override
	protected void finalize() throws Throwable {
		revertAll();
		super.finalize();
	}
}