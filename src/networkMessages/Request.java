package networkMessages;

import java.io.Serializable;

public class Request implements Serializable{
	private long lastUpdated;
	
	public Request(long lastUpdated){
		this.lastUpdated = lastUpdated;
	}
	
	public long getLastUpdated(){
		return this.lastUpdated;
	}

}
