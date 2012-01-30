package com.hekabe.cassandra.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.hekabe.cassandra.cluster.CassandraCluster;
import com.hekabe.cassandra.instance.StaticCassandraInstance;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.SshException;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.connection.ChannelOutputStream;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.transport.IgnoreHostKeyVerification;

public class YCSBWorkload {

	private static Logger _log = Logger.getLogger(YCSBWorkload.class);

	private String username;
	private String password;
	private String ip;
	
	//workload parameters
	final static String RECORD_COUNT = "recordcount";
	final static String OPERATION_COUNT = "operationcount";
	final static String WORKLOAD = "workload";
	final static String READ_ALL_FIELDS = "readallfields";
	final static String READ_PROPORTION = "readproportion";
	final static String UPDATE_PROPORTION = "updateproportion";
	final static String SCAN_PROPORTION = "scanproportion";
	final static String INSERT_PROPORTION = "insertproportion";
	final static String REQUEST_DISTRIBUTION = "requestdistribution";
	
	private HashMap<String, String> parameters = new HashMap<String, String>();
		
	
	public YCSBWorkload(String ip, String username, String password){
		this.ip = ip;
		this.username = username;
		this.password = password;
		
		//default data
		parameters.put(RECORD_COUNT, "1000");
		parameters.put(OPERATION_COUNT, "1000");
		parameters.put(WORKLOAD, "com.yahoo.ycsb.workloads.CoreWorkload");
		parameters.put(READ_ALL_FIELDS, "true");
		parameters.put(READ_PROPORTION, "0.5");
		parameters.put(UPDATE_PROPORTION, "0.5");
		parameters.put(SCAN_PROPORTION, "0.5");
		parameters.put(UPDATE_PROPORTION, "0");
		parameters.put(INSERT_PROPORTION, "0");
		parameters.put(REQUEST_DISTRIBUTION, "zipfian");
	}
	
	public void setParameter(String key, String value){
		parameters.put(key, value);
	}
	
	public String getParameter(String key){
		return parameters.get(key);
	}
	/**
	 * 
	 * @return String for ycsb call
	 */
	String buildParameterString(){
		StringBuffer parameterlist = new StringBuffer();
		for(Map.Entry<String, String> entry : parameters.entrySet()){
			parameterlist.append("-p");
			parameterlist.append(" " + entry.getKey());
			parameterlist.append(" " + entry.getValue());
			parameterlist.append(" ");
		}
		
		return parameterlist.toString();
	}
	
	public void loadBenchmarkData(CassandraCluster cluster) throws IOException{
		
		//connect ssh
		
		PasswordAuthenticationClient authClient = new PasswordAuthenticationClient();
		authClient.setUsername(username);
		authClient.setPassword(password);
		
		SshClient sshClient = new SshClient();
		sshClient.connect(ip, new IgnoreHostKeyVerification());
		
		int authResult = sshClient.authenticate(authClient);
		if (authResult != AuthenticationProtocolState.COMPLETE) {
			throw new SshException(
					"AuthenticationProtocolState is not COMPLETE");
		}
		_log.info("authentication complete");
		
		SessionChannelClient session = sshClient.openSessionChannel();
		ChannelOutputStream out = null;

		session.requestPseudoTerminal("gogrid", 80, 24, 0, 0, "");
		if (session.startShell()) {
			out = session.getOutputStream();
			
			//runycsb.sh hosts workloadfile parameters downloadfilename
			
			String hosts = "";
			
			for(String s : cluster.getPublicIps()){
				hosts = hosts + s + ",";
			}
			hosts = hosts.substring(0, hosts.length()-1);
			
			String workloadfile = "workloads/workloada";
			String parameters = buildParameterString();
			String downloadFileName = "filename";
			
			_log.info("touch test &");
			out.write("touch test &\n".getBytes());
			out.write(("runycsb.sh " + hosts + " " + workloadfile + " " + parameters + " " + downloadFileName + "\n").getBytes());
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			out.close();
		}
	}
	
	public void runWorkload(CassandraCluster cluster){
		
	}
}
