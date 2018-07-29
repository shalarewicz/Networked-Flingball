package flingball;

/**
 * A <code>RequestListener</code> sends requests to the connected flingball server. 
 * @author Stephan Halarewicz
 */
public interface RequestListener {

	/**
	 * Sends the specified <code>request</code> to the flingball server. 
	 * @param request The request to be sent to the server. 
	 */
	public void onRequest(String request);
}
