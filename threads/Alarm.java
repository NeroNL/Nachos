package nachos.threads;

import nachos.machine.*;
import java.util.*;
import nachos.security.Privilege;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
		lock = new Lock();
		wakeTime = new LinkedList<Integer>();
		a = new LinkedList<Semaphore>();
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		/*String name = "main";
		System.out.println("its name is: " + KThread.currentThread().getName());
		//if(name.equals(KThread.currentThread().getName()))
				System.out.println("its waketime is: " + wakeTime);
		if(wakeTime < Machine.timer().getTime()){
			a.V();
		}
		else{

			KThread.currentThread().yield();
		}*/

		int i = 0;
		Integer curTime = (int)(long)Machine.timer().getTime();
		while(i < wakeTime.size()){
			Integer wake = wakeTime.get(i);
			if(curTime > wake){
				Semaphore b = a.get(i);
				b.V();
				wakeTime.remove(i);
				a.remove(i);
			}
			i++;
		}
		KThread.currentThread().yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)

		if(x <= 0)
			return ;

		/*wakeTime = Machine.timer().getTime() + x;
		lock.acquire();
		a.P();
		lock.release();*/
		long waketime = Machine.timer().getTime()+x;
		Integer cT = (int)(long)waketime;

		wakeTime.add(cT);
		Semaphore cur = new Semaphore(0);
		a.add(cur);
		cur.P();
	}

	private Lock lock;
	private LinkedList<Integer> wakeTime;
	private static LinkedList<Semaphore> a;



	/*public static void selftTest() {
	    KThread t1 = new KThread(new Runnable() {
	        public void run() {
	            long time1 = Machine.timer().getTime();
	            int waitTime = 10000;
	            System.out.println("Thread calling wait at time:" + time1);
	            ThreadedKernel.alarm.waitUntil(waitTime);
	            System.out.println("Thread woken up after:" + (Machine.timer().getTime() - time1));
	            Lib.assertTrue((Machine.timer().getTime() - time1) > waitTime, " thread woke up too early.");
	            
	        }
	    });
	    t1.setName("T1");
	    t1.fork();
	    t1.join();
	}*/

	public static void selfTest() {
        System.out.println("****START ALARM TESTING****");
        
        final Alarm alarm = new Alarm();
        
        KThread t1 = new KThread(new Runnable() {
	        public void run() {
	            for (int k=0; k<6000; k++) {
	                if (k % 1000 == 0)
	                    System.out.println("T1 at " + k / 1000);
	                else if (k == 1499) {
	                    System.out.println("putting T1 to waiting queue until 1000 ticks at "+Machine.timer().getTime());
	                    alarm.waitUntil(1000);
	                    System.out.println("T1 after 1000 ticks at "+Machine.timer().getTime());
	                }
	            }
	        }
	    });
        
       KThread t2 = new KThread(new Runnable() {
            public void run() {
            	for (int k = 0; k<5000; k++)
                    if (k % 1000 == 0) {
                        System.out.println("T2 at "+ k / 1000);
                    }
                    else if (k == 2499) {
						System.out.println("putting T2 to waiting queue until 200 ticks at "+Machine.timer().getTime());
						alarm.waitUntil(200);
						System.out.println("T2 after 200 ticks at "+Machine.timer().getTime());
                    }
            }
        });
        
        KThread t3 = new KThread(new Runnable() {
            public void run() {
            	for (int k = 0; k<2500; k++)
                    if (k % 1000 == 0) {
                        System.out.println("T3 at "+ k / 1000);
                    } else if (k==1501) {
                        System.out.println("putting T3 in queue at "+Machine.timer().getTime()+" for 0 ticks");
                        alarm.waitUntil(0);
                        System.out.println("T3 after 0 ticks at "+Machine.timer().getTime());
                    }
            }
        });
        t1.setName("T1");
       	t2.setName("T2");
        t3.setName("T3");
        
        t1.fork();
        t2.fork();
        t3.fork();
        
        t1.join();
        t2.join();
        t3.join();
        
        System.out.println("****ALARM TESTING FINISH****");
    }
}
