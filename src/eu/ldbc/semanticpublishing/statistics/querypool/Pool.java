package eu.ldbc.semanticpublishing.statistics.querypool;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A class storing collection of items. On each invocation of setItemUnavailable() for a specific item,
 * availability status of all other items is tested, and if all items are 'unavailable', their status is reset to 'available'.
 */
public class Pool {
	private int unavailableItemsCount;
	private ArrayList<PoolItem> items;
	private boolean inProgress;
	private final AtomicLong totalStartsCount;
	private final AtomicLong totalResetsCount;
	
	public Pool(String poolDefinition, AtomicLong totalStartsCount, AtomicLong totalResetsCount) {
		this.unavailableItemsCount = 0;
		this.items = new ArrayList<PoolItem>();
		this.inProgress = false;
		this.totalStartsCount = totalStartsCount;
		this.totalResetsCount = totalResetsCount;
		initialize(poolDefinition);
	}
	
	public void initialize(String s) throws IllegalArgumentException, NumberFormatException {
		if (s.trim().isEmpty()) {
//			System.out.println("No query pools have been detected, continuing with default behavior...");
			return;
		}
		
		if (!validateInitString(s)) {
			throw new IllegalArgumentException(s + ", check definitions.properties parameter : queryPools...");
		}
		
		String[] tokens = s.split("\\}");
				
		for (int i = 0; i < tokens.length; i++) {
			String token = tokens[i].trim();
			
			if (token.length() == 0) {
				continue;
			}
				
			token = token.replace("{", "");

			String[] tokens2 = token.split(",");
			for (int j = 0; j < tokens2.length; j++) {
				int itemId = Integer.parseInt(tokens2[j].trim());
				addItem(new PoolItem(itemId));
			}
		}	
	}
	
	private boolean validateInitString(String s) {
		int poolDefinitionStart = 0;
		int poolDefinitionEnd = 0;
		int digits = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '{') {
				poolDefinitionStart++;
			}
			if (s.charAt(i) == '}') {
				poolDefinitionEnd++;
			}
			
			if (poolDefinitionStart > poolDefinitionEnd) {
				if (s.charAt(i) == ' ' || s.charAt(i) == ',' || s.charAt(i) == '{' || s.charAt(i) == '}') {
					continue;
				}
				if (!isNumeric(s.charAt(i))) {
					return false;
				} else {
					digits++;
				}
			}
		}
		
		if ((poolDefinitionStart == 0) || (poolDefinitionEnd == 0)) {
			return false;
		}
		
		if (poolDefinitionStart > 1 || poolDefinitionEnd > 1) {
			return false;
		}
		
		//empty pools are not allowed
		if (digits == 0) {
			return false;
		}
		
		return (poolDefinitionStart == poolDefinitionEnd);
	}
	
	private boolean isNumeric(char ch) {
		if (ch != '0' && ch != '1' && ch != '2' && 
			ch != '3' && ch != '4' && ch != '5' && 
			ch != '6' && ch != '7' && ch != '8' && ch != '9') {
			return false;
		}
		return true;
	}	
	
	public void addItem(PoolItem item) {
		items.add(item);
	}
	
	public PoolItem getItem(int itemId) {
		for (PoolItem item : items) {
			if (item.getId() == itemId) {
				return item;
			}
		}
		return null;
	}
	
	public boolean hasItem(int id) {
		for (PoolItem item : items) {
			if (item.getId() == id) {
				return true;
			}
		}
		return false;
	}
	
	public boolean itemIsAvailable(int itemId) {
		for (PoolItem item : items) {
			if (item.getId() == itemId) {
				return item.isAvailable();
			}
		}
		return false;
	}

	public synchronized void setItemUnavailable(int itemId) {
		PoolItem item = getItem(itemId);
		if (item != null) {
			if (unavailableItemsCount == 0) {
				this.inProgress = true;
				totalStartsCount.incrementAndGet();				
			}
			item.setUnavailable();
		}
	}	
	
	public synchronized void incrementUnavailableItemsCount() {
		unavailableItemsCount++;
		checkAndResetAllItems();
	}
	
	private void checkAndResetAllItems() {
		if (unavailableItemsCount >= items.size()) {		
			setAllItemsAvailable();
			unavailableItemsCount = 0;
		}		
	}
		
	private void setAllItemsAvailable() {
		this.inProgress = false;
		totalResetsCount.incrementAndGet();
		for (PoolItem item : items) {
			item.setAvailable();
		}
	}
	
	/**
	 * @param itemId - id of the item to be set unavailable
	 * @return - true if operation succeeded. false - if item was not available 
	 */
	public synchronized boolean checkAndSetItemUnavailable(int itemId) {
		if (items.size() > 0) {
			if (itemIsAvailable(itemId)) {
				setItemUnavailable(itemId);
				return true;
			} else {
				return false;
			}
		}
		
		//assuming no pool is configured, skipping pool execution			
		return true;
	}
	
	public synchronized void releaseUnavailableItem(int itemId) {
		if (items.size() > 0) {
			incrementUnavailableItemsCount();
		}	
	}	

	public int getItemsCount() {
		return items.size();
	}
	
	public boolean getInProgress() {
		return inProgress;
	}
	
	public void showPoolItems() {
		for (PoolItem item : items) {
			System.out.println("\t" + item.toString());
		}
	}
}