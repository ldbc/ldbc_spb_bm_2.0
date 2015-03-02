package eu.ldbc.semanticpublishing.statistics.querypool;

public class PoolItem {
	protected int id;
	private boolean available;
	
	public PoolItem(int id) {
		this.id = id;
		this.available = true;
	}

	protected int getId() {
		return id;
	}

	protected void setId(int id) {
		this.id = id;
	}
	
	protected boolean isAvailable() {
		return available;
	}

	protected void setAvailable() {
		this.available = true;
	}
	
	protected void setUnavailable() {
		this.available = false;
	}
	
	@Override
	public String toString() {
		return "PoolItem [id=" + id + ", av=" + available + "]";
	}
}
