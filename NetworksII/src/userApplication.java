import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.time.*;
import java.util.*;
import javax.sound.sampled.*;

import java.io.*;

public class userApplication {
	
	public static void main(String[] args) throws Exception {
		
		// Set session parameters before starting
		
		int server_port = 38011;
		int client_port = 48011;
		//Echo packets info
		String delay_echo_code = "E9667";
		String no_delay_echo_code = "E0000";
		int echo_run_time = 1;
		//Sound packets info
		String audio_code = "A0393";
		String song_number= "L99";
		String mode ="F"; //Frequency Generator or Song
		int sound_packets = 999;
		String dpcm_audio_code = audio_code + song_number + mode + String.valueOf(sound_packets) ;
		String aq_dpcm_audio_code = audio_code + "AQ" + song_number + "F" + String.valueOf(sound_packets);
		//Image packets info
		String image_code = "M9334";
		String camera_1_code = image_code + "FLOW=ONCAM=FIX";
		String camera_2_code = image_code + "FLOW=ONCAM=PTZ";
		//OBD-II
		String OBD_code = "V9066";
		int obd_run_time = 8;
		
		//Uncomment a method and run to start getting the packets associated with each task
		
		start_main_graphics();
		
//		get_ithakicopter_telemetry();
		
//		get_echo(delay_echo_code,server_port,client_port,echo_run_time);	//Gets echo with delay requests for as long as specified
//		get_echo(no_delay_echo_code,server_port,client_port,echo_run_time);	//Gets echo without delay requests for as long as specified
		
//		get_dpcm_audio(dpcm_audio_code,server_port,client_port,sound_packets);
//		get_aq_dpcm_audio(aq_dpcm_audio_code,server_port,client_port,sound_packets);
//		
		get_image(camera_1_code,server_port,client_port);
//		get_image(camera_2_code,server_port,client_port);
//		
//		getTemp(delay_echo_code,server_port,client_port);
//		
//		get_OBD(OBD_code,server_port,client_port,obd_run_time);
		
		end_main_graphics();
		
		
	}
	
	//END OF MAIN
	//Methods
	
	//Gets echo packets for the duration specified
	public static void get_echo(String requestCode,int sPort,int cPort,int time_mins)throws Exception{
		DatagramSocket s = new DatagramSocket();
		String packetInfo = requestCode;
		byte[] txbuffer = packetInfo.getBytes();
		int serverPort = sPort;
		byte[] hostIP = { (byte)155,(byte)207,(byte)18,(byte)208 };
		InetAddress hostAddress = InetAddress.getByAddress(hostIP);
		DatagramPacket p = new DatagramPacket(txbuffer,txbuffer.length, hostAddress,serverPort);
		
		int clientPort = cPort;
		DatagramSocket r = new DatagramSocket(clientPort);
		r.setSoTimeout(2000);
		byte[] rxbuffer = new byte[2048];
		DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
		
		long start_run_time = System.currentTimeMillis();		//Holds the time we started receiving packets
		long run_time_elapsed =0;								//Holds the time elapsed since we started receiving packets
		long start_packet_timer = 0;							//Holds the time we sent a packet request
		long stop_packet_timer = 0;								//Holds the time it took for a packet to arrive
		ArrayList<Long> packet_time = new ArrayList<Long>();	//Holds the response time for each packet
		ArrayList<Float> time_prob = new ArrayList<Float>();	//Holds the probability of the time, for the corresponding time in packet_time Arraylist
		int packet_ctr = 0;										//Holds the overall packets sent
		
		float a = 0.875f;				//a,b,c used in the calculations of SRTT,RTO and ó (VAR), as specified in IETF RFCs
		float b = 0.275f;
		int c=4;
		
		
		String response_txt="Packet(#), Response Time (ms)\n"; //Needed to output response time into a txt file
		
		String thrpt_32_txt="Time(sec), Throughput(bits/sec)\n";//Needed to output throughput time into a txt file
		
		String prob_txt="Response Time(ms), Probability(%)\n";	//Needed to output probability of response times into a txt file
		
		String SRTT_txt="Packet(#), SRTT\n";						//Needed to output the SRTT values
		String RTO_txt = "Packet(#), RTO\n";						//Needed to output the RTO values
		String VAR_txt = "Packet(#), Var\n";						//Needed to output the ó (VAR) values
		
		
		
		//Graphics
		if(requestCode == "E0000") {
			start_no_delay_echo_graphics();
		}
		else {
			start_delay_echo_graphics();
		}
		
		//Send,receive and time packets
		System.out.println("Please wait while the packets are loading...");
		while (run_time_elapsed < time_mins*60*1000) {
			try {
				
				s.send(p);
				start_packet_timer = System.currentTimeMillis();
				r.receive(q);
				stop_packet_timer = System.currentTimeMillis() - start_packet_timer; //Counts how long it took for the packet to arrive
				
				run_time_elapsed = System.currentTimeMillis() - start_run_time;	//Counts the total run time of the packet requests
				
				packet_ctr +=1; 		//Increases the packet count by 1
				
				packet_time.add(stop_packet_timer); //Adds the response time in the arrayList
				
				String message = new String(rxbuffer,0,q.getLength());
				System.out.println(message);
				
				
			} catch (Exception x) {
				System.out.println(x);
				start_run_time+=2000;
			}
		}
		
		//Calculations for throughput 32 sec
		
		long sec=0;
		long cur_thrpt=0;
		int index_start=0;
		int index_end=0;
		long sec_start=0;
		long sec_end=0;
		
		for(int i=0;;i++) {			//Initialize index value, from 0 to 8 seconds
			sec_end+=packet_time.get(i).longValue();
			if(sec_end >= 32000) {
				index_end = i; 
				break;
			}
		}
		
		while(true) {
			cur_thrpt = (index_end-index_start)*32*8 / ((sec_end-sec_start)/1000);
			thrpt_32_txt += sec_end/1000 + ", " + cur_thrpt+"\n";
			
			sec=sec_end;
			sec += 1000;
			
			while(sec_end<sec) {
				index_end += 1;
				if(index_end>=packet_time.size()) {
					break;
				}
				sec_end += packet_time.get(index_end).longValue();
			}
			
			if(index_end>=packet_time.size()) {
				break;
			}
			
			while(sec_end-sec_start>32000) {
				sec_start += packet_time.get(index_start).longValue();
				index_start += 1;
			}
			
		}
		
		
		//Calculations for probability of each value
		
		ArrayList<Long> packet_time_sorted = new ArrayList<>(packet_time);	//Creates a new arraylist to be edited, so that we don't previous information
		Collections.sort(packet_time_sorted); //Sort the arraylist
		
		int loop_index=0;
		while(loop_index<packet_time_sorted.size()) {
			int ctr=1;					//Counts the instances of a certain number
			while(true) {
				if((loop_index+1<packet_time_sorted.size()) && (packet_time_sorted.get(loop_index).longValue()==packet_time_sorted.get(loop_index+1).longValue())) {	//checks if the value after i is equal to the value at i
					ctr+=1;		//Increases the instance counter
					packet_time_sorted.remove(loop_index+1);	//removes the duplicate value
				}
				else {break;}
			}
			float prob = (float)(100*ctr)/(packet_ctr);		//Calculates the % probability of the value i, after we have counted how many times it was encountered
			time_prob.add(prob);							//Adds the probability in the probability arraylist
			prob_txt+=packet_time_sorted.get(loop_index) + ", "+time_prob.get(loop_index)+"\n";	//	Edits the txt that we are going to output in the end
					
			loop_index += 1;						//Goes to the next number
		}
		
		
		//Calculations for SRTT, RTO, ó (VAR)
		
		float[] SRTT = new float[packet_time.size()];	//Holds the SRTT values
		float[] VAR = new float[packet_time.size()];	//Holds the ó (VAR) values
		float[] RTO = new float[packet_time.size()];	//Holds the RTO values
		
		for(int i=0;i<packet_time.size();i++) {		//Calculates the SRTT values
			if(i==0) {
				SRTT[i] = packet_time.get(i).floatValue();	//From IETF RFCs
			}
			else {
				SRTT[i] = a*SRTT[i-1]+(1-a)*packet_time.get(i).floatValue();
			}
			SRTT_txt += i + ", " + SRTT[i] +"\n";
		}
		
		for(int i=0;i<packet_time.size();i++) {		//Calculates the VAR values
			if(i==0) {
				VAR[i] = packet_time.get(i).floatValue()/2;	//From IETF RFCs
			}
			else {
				VAR[i] = b*VAR[i-1]+(1-b)*Math.abs((SRTT[i]-packet_time.get(i).floatValue())); //Math.abs = absolute value
			}
			VAR_txt += i + ", " + VAR[i] +"\n";
		}
		
		for(int i=0;i<packet_time.size();i++) {		//Calculates the RTO values
			RTO[i] = SRTT[i] + c*VAR[i];
			RTO_txt += i + ", " + RTO[i] +"\n";
		}
		
		//Calculations for mesi timi kai diaspora
		
		float mesi_timi=0;
		float diaspora=0;
		float mesi_timi_tetragonou=0;
		
		
		String mesi_timi_txt;
		String diaspora_txt;
		
		
		for(int i=0;i<packet_time.size();i++) {		//Calculate mesi timi
			mesi_timi += packet_time.get(i).floatValue();
		}
		mesi_timi = mesi_timi/packet_time.size();
		mesi_timi_txt = ""+mesi_timi;
		
		for(int i=0;i<packet_time.size();i++) {		//Calculate diaspora
			mesi_timi_tetragonou += packet_time.get(i).floatValue()*packet_time.get(i).floatValue();	
		}
		mesi_timi_tetragonou = mesi_timi_tetragonou/packet_time.size();
		diaspora = mesi_timi_tetragonou -  mesi_timi*mesi_timi;
		diaspora_txt=""+diaspora;
		
		
		//Creating the output txt for the response time
		for(int i=0;i<packet_time.size();i++) {
			response_txt += i+", "+packet_time.get(i).longValue()+"\n";
		}
		
		//Output info files
		if(requestCode == "E0000") {
			stringToTextFile("SRTT_NO_delay",SRTT_txt);
			stringToTextFile("VAR_NO_delay",VAR_txt);
			stringToTextFile("RTO_NO_delay",RTO_txt);
			stringToTextFile("response_time_NO_delay",response_txt);
			stringToTextFile("throughput_32_NO_delay",thrpt_32_txt);
			stringToTextFile("probability_NO_delay",prob_txt);
			stringToTextFile("mesi_timi_NO_delay",mesi_timi_txt);
			stringToTextFile("diaspora_NO_delay",diaspora_txt);
		}else {
			stringToTextFile("SRTT_WITH_delay",SRTT_txt);
			stringToTextFile("VAR_WITH_delay",VAR_txt);
			stringToTextFile("RTO_WITH_delay",RTO_txt);
			stringToTextFile("response_time_WITH_delay",response_txt);
			stringToTextFile("throughput_32_WITH_delay",thrpt_32_txt);
			stringToTextFile("probability_WITH_delay",prob_txt);
			stringToTextFile("mesi_timi_WITH_delay",mesi_timi_txt);
			stringToTextFile("diaspora_WITH_delay",diaspora_txt);
		}

		//Graphics
		end_echo_graphics();
		
		r.close();
		s.close();
	};
	
	public static void getTemp(String requestCode,int sPort,int cPort)throws Exception {
		DatagramSocket s = new DatagramSocket();
		String packetInfo = requestCode+"T00";
		byte[] txbuffer = packetInfo.getBytes();
		int serverPort = sPort;
		byte[] hostIP = { (byte)155,(byte)207,(byte)18,(byte)208 };
		InetAddress hostAddress = InetAddress.getByAddress(hostIP);
		DatagramPacket p = new DatagramPacket(txbuffer,txbuffer.length, hostAddress,serverPort);
		
		int clientPort = cPort;
		DatagramSocket r = new DatagramSocket(clientPort);
		r.setSoTimeout(2000);
		byte[] rxbuffer = new byte[2048];
		DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
		
		s.send(p);
		r.receive(q);
		String message = new String(rxbuffer,0,q.getLength());
		
		stringToTextFile("temp_00",message);
		
	}
	
	//Converts a string into a new text file
	public static void stringToTextFile(String name,String input)throws IOException { //Outputs input String as a text file with the specified name.
		BufferedWriter writer = new BufferedWriter(new FileWriter(name +".txt"));
	    writer.write(input);
	     
	    writer.close();
	}
	
	public static void get_dpcm_audio(String requestCode,int sPort,int cPort,int sound_packets)throws Exception {
		//Open communications
		DatagramSocket s = new DatagramSocket();
		String packetInfo = requestCode;
		byte[] txbuffer = packetInfo.getBytes();
		int serverPort = sPort;
		byte[] hostIP = { (byte)155,(byte)207,(byte)18,(byte)208 };
		InetAddress hostAddress = InetAddress.getByAddress(hostIP);
				
		DatagramPacket p = new DatagramPacket(txbuffer,txbuffer.length, hostAddress,serverPort);
				
		int clientPort = cPort;
		DatagramSocket r = new DatagramSocket(clientPort);
		r.setSoTimeout(200);
				
		byte[] rxbuffer = new byte[128]; //128 bytes in a packet
		DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
				
		//Creating Audio Buffer
		byte[] audio_elastic_buffer = new byte[256*sound_packets]; //128 bytes per packet
		
		String samples_txt="";
		String deltas_txt="";
		
		//Graphics
		start_dpcm_graphics();
		
		//Send the request
		s.send(p);
		//Receive the packets
		for(int i =0;i<sound_packets;i++) {
			try {
				r.receive(q);
				System.out.println("Packet "+ i + " loaded!");
				MusicSamples mySamples = DPCM_decoder(rxbuffer,0,1);	//Creating a MusicSamples object so that we can output both samples and deltas
				byte[] decoded = mySamples.getSamples();
				int[] deltas = mySamples.getDeltas();
				//Transfer from decoded to elastic_buffer
				for(int j=0;j<decoded.length;j++) {
					audio_elastic_buffer[j+i*256] = decoded[j]; //j+i*128 so that we don't override
				}
				//Adding samples and deltas to txt so that we can output them
				for(int j=0;j<decoded.length;j++) {
					samples_txt+=decoded[j]+"\n";
				}
				for(int j=0;j<deltas.length;j++) {
					deltas_txt+=deltas[j]+"\n";
				}
			}catch(Exception e){
				System.out.println(e);
			}
		}
			
		//Output to txt files
		stringToTextFile("DPCM_samples",samples_txt);
		stringToTextFile("DPCM_deltas",deltas_txt);
		
		//Graphics
		end_dpcm_graphics();
		
		r.close();
		s.close();
				
		AudioFormat linearPCM = new AudioFormat(8000,8,1,true,false);
		SourceDataLine lineOut = AudioSystem.getSourceDataLine(linearPCM);
		lineOut.open(linearPCM,32000);
		lineOut.start();
		lineOut.write(audio_elastic_buffer,0,audio_elastic_buffer.length);
		lineOut.stop();
		lineOut.close();
		
	}
	
	public static MusicSamples DPCM_decoder(byte[] dpcm_bytes,int mean,int step) {
		int m = mean;
		int beta = step;
		
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        
		byte[] buffer_out = new byte[dpcm_bytes.length*2];
		int[] deltas = new int[buffer_out.length];
		
        int LSNib;
        int MSNib;
        int current_byte;
        int index =0;
        for(int i=0;i<dpcm_bytes.length;i++) {
        	index = 2*i;
        	current_byte = dpcm_bytes[i];
            LSNib = (current_byte      ) & 15;
            MSNib = (current_byte >>> 4) & 15;   
            
            int delta1 = (MSNib - 8) * beta;
            int delta2 = (LSNib - 8) * beta;
            deltas[index] = delta1;
            deltas[index+1] = delta2;
        }
        
        
        //Assuming that the sum of all differences in a packet is 0 Ódeltas[i]=0, we can calculate the first sample
        int sum=0;
        for(int i=0;i<deltas.length;i++) {
        	for(int j=0;j<=i;j++) {
        		sum+=deltas[j];
        	}
        }
        
        int x0 = -(sum)/256;
        buffer_out[0] = (byte) x0;
        
        //Calculating the rest of the samples
        for(int i =1; i<buffer_out.length;i++) {
        	buffer_out[i] = (byte)(deltas[i-1] + (int)buffer_out[i-1]);
        }
        
        MusicSamples output = new MusicSamples(buffer_out,deltas,m,beta);
        
        return output;
		
	}
	
	public static void get_aq_dpcm_audio(String requestCode,int sPort,int cPort,int sound_packets)throws Exception {
		//Open communications
		DatagramSocket s = new DatagramSocket();
		String packetInfo = requestCode;
		byte[] txbuffer = packetInfo.getBytes();
		int serverPort = sPort;
		byte[] hostIP = { (byte)155,(byte)207,(byte)18,(byte)208 };
		InetAddress hostAddress = InetAddress.getByAddress(hostIP);
				
		DatagramPacket p = new DatagramPacket(txbuffer,txbuffer.length, hostAddress,serverPort);
				
		int clientPort = cPort;
		DatagramSocket r = new DatagramSocket(clientPort);
		r.setSoTimeout(2000);
				
		byte[] rxbuffer = new byte[132]; //132 bytes in a packet
		DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
				
		//Creating Audio Buffer
		byte[] audio_elastic_buffer = new byte[2*256*sound_packets]; //2*256 bytes of sound per packet
		
		String samples_txt="";
		String delta_txt="";
		String mean_txt="";
		String beta_txt="";
		
		//Graphics
		start_aq_dpcm_graphics();
		
		//Send the request
		s.send(p);
		//Receive the packets
		for(int i =0;i<sound_packets;i++) {
			try {
				r.receive(q);
				System.out.println("Packet "+ i + " loaded!");
				MusicSamples mySamples = AQ_DPCM_decoder(rxbuffer);
				byte[] decoded = mySamples.getSamples();
				byte[] delta = mySamples.getDelta();
				int mean = mySamples.getMean();
				int beta = mySamples.getBeta();
				
				//Transfer from decoded to elastic_buffer
				for(int j=0;j<decoded.length;j++) {
					audio_elastic_buffer[j+i*512] = decoded[j]; //j+i*256 so that we don't override
				}
				
				//Edit txt variables so that we can output
				for(int j=0;j<decoded.length;j++) {
					samples_txt+=decoded[j]+"\n";
				}
				for(int j=0;j<delta.length;j++) {
					delta_txt+=delta[j]+"\n";
				}
				beta_txt+=beta +"\n";
				mean_txt+=mean+"\n";
			}catch(Exception e){
				System.out.println(e);
			}
		}
		
		//Output to txt files
		
		stringToTextFile("AQ_DPCM_samples",samples_txt);
		stringToTextFile("AQ_DPCM_deltas",delta_txt);
		stringToTextFile("AQ_DPCM_mean",mean_txt);
		stringToTextFile("AQ_DPCM_beta",beta_txt);
		
		//Graphics
		end_aq_dpcm_graphics();
		
		r.close();
		s.close();
				
		AudioFormat linearPCM = new AudioFormat(8000,16,1,true,false);
		SourceDataLine lineOut = AudioSystem.getSourceDataLine(linearPCM);
		lineOut.open(linearPCM,32000);
		lineOut.start();
		lineOut.write(audio_elastic_buffer,0,audio_elastic_buffer.length);
		lineOut.stop();
		lineOut.close();
		
	}
	
	public static MusicSamples AQ_DPCM_decoder(byte[] input) {
		int mean = bytesToInt(input[1],input[0]);
		int beta = bytesToInt(input[3],input[2]);
		
		byte[] delta = new byte[512];
		byte[] sound_bytes = new byte[128];
		byte[] buffer_out = new byte[512];
		
		for(int i=4;i<input.length;i++) {
			sound_bytes[i-4]=input[i];
		}
		
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		
        int LSNib;
        int MSNib;
        int current_byte;
        for(int i=0;i<sound_bytes.length;i++) {
        	int index = 4*i;
        	current_byte = sound_bytes[i];
            LSNib = (current_byte      ) & 15;
            MSNib = (current_byte >>> 4) & 15;   
            
            int delta1 = (MSNib - 8) *beta+mean;
            delta[index] = (byte)(delta1 & 0xFF);
            delta[index+1] = (byte)((delta1>>>8) & 0xFF);
            
            int delta2 = (LSNib - 8) *beta+mean;
            delta[index+2] = (byte)(delta2 & 0xFF);
            delta[index+3] = (byte)((delta2>>>8) & 0xFF);
            
        }
        
        
        for(int i=2;i<512;i+=2) {
            
            byte sample;
            sample = (byte)(bytesToInt(delta[i-2],delta[i-1])+bytesToInt(buffer_out[i-2],buffer_out[i-1]));
            
            buffer_out[i]= (byte) (sample & 0xFF);
            buffer_out[i+1] = (byte)((sample>>>8) & 0xFF);
            
        }
        
        MusicSamples output = new MusicSamples(buffer_out,delta,mean,beta);
        
        return output;
		
	}
	
	public static int bytesToInt(byte h,byte l) {
		int i = (h << 8) | (l & 0xff);
		return i;
	}
	
	public static void get_image(String requestCode,int sPort,int cPort)throws Exception {
		DatagramSocket s = new DatagramSocket();
		String packetInfo = requestCode;
		byte[] txbuffer = packetInfo.getBytes();
		int serverPort = sPort;
		byte[] hostIP = { (byte)155,(byte)207,(byte)18,(byte)208 };
		InetAddress hostAddress = InetAddress.getByAddress(hostIP);
		DatagramPacket p = new DatagramPacket(txbuffer,txbuffer.length, hostAddress,serverPort);
		
		int clientPort = cPort;
		DatagramSocket r = new DatagramSocket(clientPort);
		r.setSoTimeout(3300);
		byte[] rxbuffer = new byte[128];
		DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
		
		OutputStream cImageout;
		
		if(requestCode.charAt((requestCode.length()-1))=='Z') {
			start_ptzcam_graphics();
			cImageout = new FileOutputStream("PTZ_image.jpeg");
		}else {
			start_fixcam_graphics();
			cImageout = new FileOutputStream("FIX_image.jpeg");
		}
		System.out.println("Please wait while the image is loading...");
		
		s.send(p);
		
		String next = "NEXT";
		byte[] next_bytes = next.getBytes();
		DatagramPacket n = new DatagramPacket(next_bytes,next_bytes.length, hostAddress,serverPort);
		
		
		while(true) {
			try {
				r.receive(q);
				cImageout.write(rxbuffer);
				s.send(n);
			}catch (Exception e){
				break;
			}
			if(rxbuffer[rxbuffer.length-2]==0xff && rxbuffer[rxbuffer.length-1] == 0xd9 ) {
				break;
			}
		}
		
		end_cam_graphics();
		
		cImageout.close();
		r.close();
		s.close();
	}

	public static void get_ithakicopter_telemetry() throws Exception {
		
		int clientPort = 48078;
		DatagramSocket r = new DatagramSocket(clientPort);
		r.setSoTimeout(2000);
		byte[] rxbuffer = new byte[2048];
		DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
		
		String info="LMOTOR, RMOTOR, ALTITUDE, TEMPERATURE, PRESSURE\n"; //Needed to output into a txt file
		int[] accepted = {40,41,42,51,52,53,64,65,66,81,82,83,84,85,96,97,98,99,100,101,102};
		
		//Graphics
		start_copter_graphics();
		
		for(int i =0; i<200;i++) {
			try {
				r.receive(q);
				
				String message = new String(rxbuffer,0,q.getLength());
				System.out.println(message);
				for(int j=0;j<message.length();j++) {
					for(int k=0;k<accepted.length;k++) {
						if(j==accepted[k]) {
							info += message.charAt(j);
							if(j==42 || j==53 || j==66 || j==85) {info+=", ";}
						}
					}
				}
				info += "\n";
				
			} catch (Exception x) {
				System.out.println(x);
			}
		}
		
		end_copter_graphics();
		//Output info file
		stringToTextFile("ithakicopter_telemetry",info);
	}
	
	public static void get_OBD(String requestCode,int sPort,int cPort,int time_mins)throws Exception {

		DatagramSocket s = new DatagramSocket();
		String packetInfo = requestCode;
		byte[] txbuffer = packetInfo.getBytes();
		int serverPort = sPort;
		byte[] hostIP = { (byte)155,(byte)207,(byte)18,(byte)208 };
		InetAddress hostAddress = InetAddress.getByAddress(hostIP);
		DatagramPacket p = new DatagramPacket(txbuffer,txbuffer.length, hostAddress,serverPort);
		
		int clientPort = cPort;
		DatagramSocket r = new DatagramSocket(clientPort);
		r.setSoTimeout(2000);
		byte[] rxbuffer = new byte[11];
		DatagramPacket q = new DatagramPacket(rxbuffer,rxbuffer.length);
		
		long start_run_time = System.currentTimeMillis();
		long run_time_elapsed =0;
		
		//Graphics
		start_OBD_graphics();
		
		System.out.println("Engine run time - Intake Air Temperature - Throttle Possition - Engine RPM - Vehicle Speed - Coolant Temperature");
		String obdtxt="Engine run time - Intake Air Temperature - Throttle Possition - Engine RPM - Vehicle Speed - Coolant Temperature\n"; //Needed to output into a txt file

		int engine_run_time=0;
		int intake_air_temperature=0;
		int throttle_position=0;
		int engine_rpm=0;
		int vehicle_speed=0;
		int coolant_temperature=0; 
		
		//Send,receive and time packets
		while (run_time_elapsed < time_mins*60*1000) {
			try {
				String hex = "";
				int x;
				int y;
				
				packetInfo = requestCode + "OBD=01 1F";
				p.setData(packetInfo.getBytes());
				s.send(p);
				r.receive(q);
				for(int i=0;i<rxbuffer.length;i++) {
					hex+=(char)rxbuffer[i];
				}
				x= Integer.parseInt(hex.substring(6, 8),16);
				y= Integer.parseInt(hex.substring(9, 11),16);
				engine_run_time = 256*x+y;
				
				hex = "";
				packetInfo = requestCode + "OBD=01 0F";
				p.setData(packetInfo.getBytes());
				s.send(p);
				r.receive(q);
				for(int i=0;i<rxbuffer.length;i++) {
					hex+=(char)rxbuffer[i];
				}
				x= Integer.parseInt(hex.substring(6, 8),16);
				intake_air_temperature = x - 40;
				
				packetInfo = requestCode + "OBD=01 11";
				p.setData(packetInfo.getBytes());
				s.send(p);
				r.receive(q);
				for(int i=0;i<rxbuffer.length;i++) {
					hex+=(char)rxbuffer[i];
				}
				x= Integer.parseInt(hex.substring(6, 8),16);
				throttle_position = x*100/255;
				
				packetInfo = requestCode + "OBD=01 0C";
				p.setData(packetInfo.getBytes());
				s.send(p);
				r.receive(q);
				for(int i=0;i<rxbuffer.length;i++) {
					hex+=(char)rxbuffer[i];
				}
				x= Integer.parseInt(hex.substring(6, 8),16);
				y= Integer.parseInt(hex.substring(9, 11),16);
				engine_rpm = ((x*256)+y)/4;
				
				packetInfo = requestCode + "OBD=01 0D";
				p.setData(packetInfo.getBytes());
				s.send(p);
				r.receive(q);
				for(int i=0;i<rxbuffer.length;i++) {
					hex+=(char)rxbuffer[i];
				}
				x= Integer.parseInt(hex.substring(6, 8),16);
				vehicle_speed = x;
				
				packetInfo = requestCode + "OBD=01 05";
				p.setData(packetInfo.getBytes());
				s.send(p);
				r.receive(q);
				for(int i=0;i<rxbuffer.length;i++) {
					hex+=(char)rxbuffer[i];
				}
				x= Integer.parseInt(hex.substring(6, 8),16);
				coolant_temperature = x-40; 
				
				
				System.out.println(engine_run_time + " sec, " + intake_air_temperature + " C, " + throttle_position + "%, " + engine_rpm + " RPM, " + vehicle_speed + " km/h, " + coolant_temperature + " C");
				obdtxt+= engine_run_time + ", " + intake_air_temperature + ", " + throttle_position + ", " + engine_rpm + ", " + vehicle_speed + ", " + coolant_temperature + "\n";
				
				run_time_elapsed = System.currentTimeMillis() - start_run_time;	//Counts the total run time of the packet requests
			
			} catch (Exception x) {
				System.out.println(x);
			}
		}
		
		//Output info file
		stringToTextFile("OBD_II_parameters",obdtxt);
		
		//Graphics
		end_OBD_graphics();
		r.close();
		s.close();
	}
	
	
	//Console Graphics
	public static void sleep(int ms) {
		try
		{
		    Thread.sleep(ms);
		}
		catch(InterruptedException ex)
		{
		    Thread.currentThread().interrupt();
		}

	}
		
	public static void start_main_graphics() {
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		System.out.println("  _   _      _                      _          ___ ___                   _           _   \r\n" + 
				" | \\ | | ___| |___      _____  _ __| | _____  |_ _|_ _|  _ __  _ __ ___ (_) ___  ___| |_ \r\n" + 
				" |  \\| |/ _ \\ __\\ \\ /\\ / / _ \\| '__| |/ / __|  | | | |  | '_ \\| '__/ _ \\| |/ _ \\/ __| __|\r\n" + 
				" | |\\  |  __/ |_ \\ V  V / (_) | |  |   <\\__ \\  | | | |  | |_) | | | (_) | |  __/ (__| |_ \r\n" + 
				" |_| \\_|\\___|\\__| \\_/\\_/ \\___/|_|  |_|\\_\\___/ |___|___| | .__/|_|  \\___// |\\___|\\___|\\__|\r\n" + 
				"                                                        |_|           |__/               ");
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
	}
	
	public static void end_main_graphics() {
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		System.out.println("  _____ _           _   _             _ _      __       _ _          _ \r\n" + 
				" |_   _| |__   __ _| |_( )___    __ _| | |    / _| ___ | | | _____  | |\r\n" + 
				"   | | | '_ \\ / _` | __|// __|  / _` | | |   | |_ / _ \\| | |/ / __| | |\r\n" + 
				"   | | | | | | (_| | |_  \\__ \\ | (_| | | |_  |  _| (_) | |   <\\__ \\ |_|\r\n" + 
				"   |_| |_| |_|\\__,_|\\__| |___/  \\__,_|_|_( ) |_|  \\___/|_|_|\\_\\___/ (_)\r\n" + 
				"                                         |/                            ");
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
	}
	
	public static void start_delay_echo_graphics(){
		System.out.println(" _______________________________________________");
		System.out.println("|                                               |");
		System.out.println("|   Loading ECHO packets WITH server delay...   |");
		System.out.println("|_______________________________________________|");
		System.out.println();
	}
	
	public static void start_no_delay_echo_graphics() {
		System.out.println(" _______________________________________________");
		System.out.println("|                                               |");
		System.out.println("|  Loading ECHO packets WITHOUT server delay... |");
		System.out.println("|_______________________________________________|");
		System.out.println();
		sleep(1500);
	}
	
	public static void end_echo_graphics() {
		System.out.println(" _______________________________________________");
		System.out.println("|                                               |");
		System.out.println("|               Packets loaded! :)              |");
		System.out.println("|_______________________________________________|");
		System.out.println();
	}
	
	public static void start_dpcm_graphics() {
		System.out.println(" _______________________________________________");
		System.out.println("|                                               |");
		System.out.println("|     Loading DPCM encoded audio packets...     |");
		System.out.println("|_______________________________________________|");
		System.out.println();
		sleep(1500);
	}
	
	public static void end_dpcm_graphics() {
		System.out.println(" _______________________________________________");
		System.out.println("|                                               |");
		System.out.println("|      DPCM encoded audio packets loaded!       |");
		System.out.println("|_______________________________________________|");
		System.out.println();
	}
	
	public static void start_aq_dpcm_graphics() {
		System.out.println(" _______________________________________________");
		System.out.println("|                                               |");
		System.out.println("|    Loading AQ-DPCM encoded audio packets...   |");
		System.out.println("|_______________________________________________|");
		System.out.println();
		sleep(1500);
	}
	
	public static void end_aq_dpcm_graphics() {
		System.out.println(" _______________________________________________");
		System.out.println("|                                               |");
		System.out.println("|     AQ-DPCM encoded audio packets loaded!     |");
		System.out.println("|_______________________________________________|");
		System.out.println();
	}
	
	public static void start_ptzcam_graphics() {
		System.out.println(" _______________________________________________");
		System.out.println("|                                               |");
		System.out.println("|        Loading image from camera PTZ...       |");
		System.out.println("|_______________________________________________|");
		System.out.println();
		sleep(500);
	}
	
	public static void start_fixcam_graphics() {
		System.out.println(" _______________________________________________");
		System.out.println("|                                               |");
		System.out.println("|        Loading image from camera FIX...       |");
		System.out.println("|_______________________________________________|");
		System.out.println();
		sleep(500);
	}
	
	public static void end_cam_graphics() {
		System.out.println(" _______________________________________________");
		System.out.println("|                                               |");
		System.out.println("|                 Image loaded!                 |");
		System.out.println("|_______________________________________________|");
		System.out.println();
	}
	
	public static void start_copter_graphics() {
		System.out.println(" _______________________________________________");
		System.out.println("|                                               |");
		System.out.println("|        Loading Ithakicopter telemetry...      |");
		System.out.println("|_______________________________________________|");
		System.out.println();
	}
	
	public static void end_copter_graphics() {
		System.out.println(" _______________________________________________");
		System.out.println("|                                               |");
		System.out.println("|         Ithakicopter telemetry loaded!        |");
		System.out.println("|_______________________________________________|");
		System.out.println();
	}
	
	public static void start_OBD_graphics() {
		System.out.println(" _______________________________________________");
		System.out.println("|                                               |");
		System.out.println("|            Loading OBD parameters...          |");
		System.out.println("|_______________________________________________|");
		System.out.println();
	}
	
	public static void end_OBD_graphics() {
		System.out.println(" _______________________________________________");
		System.out.println("|                                               |");
		System.out.println("|             OBD parameters loaded!            |");
		System.out.println("|_______________________________________________|");
		System.out.println();
	}
		
	
}
