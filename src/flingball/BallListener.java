package flingball;


/**
 * BallListeners will create a new thread that takes the provided actions and then sleeps for a set amount 
 * of time before taking the provided actions again. 
 * @author shala
 *
 */
public interface BallListener {
	/**
	 * Starts a thread which sleeps for time ms between performing the given actions. 
	 * @param time time in seconds for which the thread will sleep
	 */
	public void onStart(final double time);
	
	/**
	 * Stops the thread.
	 */
	public void onEnd();
	
	/**
	 * 
	 * @return the name of the threads. 
	 */
	public String name();
}
