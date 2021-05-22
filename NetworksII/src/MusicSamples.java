
public class MusicSamples {
	private byte[] samples;
	private int[] deltas;
	private byte[] delta;
	private int mean;
	private int beta;
	public MusicSamples(byte[] a,int[] b,int c,int d) {
		
		samples = a;
		deltas =b;
		mean = c;
		beta=d;
	}
	public MusicSamples(byte[] a,byte[] b,int c,int d) {
		
		samples = a;
		delta =b;
		mean = c;
		beta=d;
	}
	public byte[] getSamples() {return samples;}
	public int[] getDeltas() {return deltas;}
	public int getMean() {return mean;}
	public int getBeta() {return beta;}
	public byte[] getDelta() {return delta;}
}
