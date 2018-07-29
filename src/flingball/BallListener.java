package flingball;


/**
 * BallListeners will create a new thread that performs the actions provided in <code>onStart()</code> 
 * and then sleeps for <code>time</code> seconds before performing the provided actions again. 
 * 
 * @author Stephan Halarewicz
 *
 */
interface BallListener {
	/**
	 * Starts a thread which sleeps for time ms between performing the given actions. 
	 * @param time time in seconds for which the thread will sleep
	 */
	public void onStart(final double time);
	
	/**
	 * Stops the ball's thread.
	 */
	public void onEnd();
	
	/**
	 * @return the name of the created thread. 
	 * @throws NullPointerException if the thread has not yet been started. 
	 */
	public String name() throws NullPointerException;
}
