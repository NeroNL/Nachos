package nachos.threads;

import nachos.machine.*;
import java.util.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	/**
	 * Allocate a new communicator.
	 */

	public Communicator() {
        lock = new Lock();
        listen_lock = new Condition(lock);
        speak_lock = new Condition(lock);
        listenning = 0;
        speaking = 0;
        text = new LinkedList<Integer>();
        isEmpty = new Condition2(lock);
        textbuffer = 0;
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word the integer to transfer.
	 */
	public void speak(int word) {
        text.add(word);
        System.out.println("text added is: " + text.peekLast());
        lock.acquire();
        
        speaking++;

        listen_lock.wake();

        while(listenning < 1){
            speak_lock.sleep();
        }

        listenning--;
//System.out.println("lock is here: " + textbuffer);
        if(textbuffer > 0){
            
            isEmpty.sleep();
        }
        System.out.println("speak returns at: " + Machine.timer().getTime());

        lock.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
        
        lock.acquire();
        listenning++;
        textbuffer++;
        speak_lock.wake();
        int returned = 0;
        while(speaking < 1)
            listen_lock.sleep(); 

        speaking--;

        isEmpty.wake();

        returned = text.removeLast();
        System.out.println("text returned is: " + returned);
        lock.release();
        textbuffer--;
        return returned;
	}

    private Lock lock;
    private Condition speak_lock;
    private Condition listen_lock;
    private int listenning;
    private int speaking;
    private LinkedList<Integer> text;
    private int textbuffer;
    private Condition2 isEmpty;
    public static int i =0;
    

	public static void selfTest(){
        final Communicator com = new Communicator();
        final long times[] = new long[4];
        final int words[] = new int[2];
        KThread speaker1 = new KThread( new Runnable () {
            public void run() {
                com.speak(4);
                times[0] = Machine.timer().getTime();
                System.out.println("time0 is: " + times[0]);
                i++;
                //System.out.println("speaker1 i is: " + i);
            }
        });
        speaker1.setName("S1");
        KThread speaker2 = new KThread( new Runnable () {
            public void run() {
                com.speak(7);
                times[1] = Machine.timer().getTime();
                System.out.println("time1 is: " + times[1]);
                i++;
                //System.out.println("speaker2 i is: " + i);
            
            }
        });
        speaker2.setName("S2");
        KThread listener1 = new KThread( new Runnable () {
            public void run() {
                words[0] = com.listen();  
                System.out.println("words0 is: " + words[0]); 
                times[2] = Machine.timer().getTime();
                System.out.println("time2 is: " + times[2]);
                i++;
                //System.out.println("listener1 i is: " + i);
            
            }
        });
        listener1.setName("L1");
        KThread listener2 = new KThread( new Runnable () {
            public void run() {
                words[1] = com.listen();
                System.out.println("words1 is: " + words[1]);
                times[3] = Machine.timer().getTime();
                System.out.println("time3 is: " + times[3]);
                i++;
                 //System.out.println("listener2 i is: " + i);
            }
        });
        listener2.setName("L2");
        
        
        listener1.fork(); 
        
        speaker1.fork(); 
        speaker2.fork(); 
        listener2.fork(); 
        speaker1.join(); 
        speaker2.join(); 
        listener1.join(); 
        listener2.join(); 
        while(i < 4) {
            
        }
        System.out.println("before check: " + Machine.timer().getTime());
        Lib.assertTrue(words[0] == 4, "Didn't listen back spoken word."); 
        Lib.assertTrue(words[1] == 7, "Didn't listen back spoken word.");
        Lib.assertTrue(times[0] > times[2], "speak returned before listen.");
        //System.out.println("before last check: " + Machine.timer().getTime());
        Lib.assertTrue(times[1] > times[3], "speak returned before listen.");
    }
}
