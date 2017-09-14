package pstb.benchmark;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

//import ca.utoronto.msrg.padres.client.Client;
import pstb.util.ClientAction;
import pstb.util.NodeRole;

public class PADRESClient{
	
	//private Client PadresClient;
	
	private DateFormat dateFormat;
	private String clientName;
	private ArrayList<NodeRole> roles;
	private ArrayList<String> brokerURIs;
	//TODO: Publisher Workload
	//TODO: Subscriber Workload
	private ArrayList<HashMap<String, String>> diary = new ArrayList<HashMap<String, String>>();
	
	public PADRESClient(){
		dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		clientName = new String();
		
	}

	public boolean initialize(String givenName) {
		return false;
	}

	public boolean shutdown() {
		return false;
	}

	public boolean connect(ArrayList<String> givenBrokerURIs) {
		return false;
	}

	public boolean disconnect() {
		return false;
	}

	public void startRun() {

	}


	public boolean handleAction(ClientAction givenAction, String predicates) {
		return false;
	}


	public void listen() {

	}

	public void addDiaryEntry() {

	}

}
