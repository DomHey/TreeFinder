package networkMessages;

import java.io.Serializable;
import java.util.ArrayList;

public class ServerResponse implements Serializable{
	private ArrayList<Response> list = new ArrayList<Response>();
	
	public ServerResponse(ArrayList<Response> list){
		this.list = list;
	}
	
	public ArrayList<Response> getList(){
		return this.list;
	}
	
}
