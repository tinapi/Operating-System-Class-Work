import java.util.*;

public class Scheduler extends Thread
{
    private Vector[] queue; // create three priority queues 
    private int timeSlice;
    private static final int DEFAULT_TIME_SLICE = 1000;

    // New data added to p161 
    private boolean[] tids; // Indicate which ids have been used
    private static final int DEFAULT_MAX_THREADS = 10000;

    // A new feature added to p161 
    // Allocate an ID array, each element indicating if that id has been used
    private int nextId = 0;
    private void initTid( int maxThreads ) {
    	tids = new boolean[maxThreads];
    	for ( int i = 0; i < maxThreads; i++ )
    	    tids[i] = false;
    }

    // A new feature added to p161 
    // Search an available thread ID and provide a new thread with this ID
    private int getNewTid( ) {
    	for ( int i = 0; i < tids.length; i++ ) {
    	    int tentative = ( nextId + i ) % tids.length;
    	    if ( tids[tentative] == false ) {
    		tids[tentative] = true;
    		nextId = ( tentative + 1 ) % tids.length;
    		return tentative;
    	    }
    	}
    	return -1;
    }

    // A new feature added to p161 
    // Return the thread ID and set the corresponding tids element to be unused
    private boolean returnTid( int tid ) {
	   if ( tid >= 0 && tid < tids.length && tids[tid] == true ) {
	    tids[tid] = false;
	    return true;
	   }
	   return false;
    }

    // A new feature added to p161 
    // Retrieve the current thread's TCB from the queue
    public TCB getMyTcb( ) {
    	Thread myThread = Thread.currentThread( ); // Get my thread object
        // iterate through 3 queues to find the specific tcb
    	for (int count = 0; count < 3; count++)
    	{
    	   synchronized( queue[count] ) {
    	    for ( int i = 0; i < queue[count].size( ); i++ ) {
    		TCB tcb = ( TCB )queue[count].elementAt( i );
    		Thread thread = tcb.getThread( );
    		if ( thread == myThread ) // if this is my TCB, return it
    		    return tcb;
    	    }
    	}
    	}
    	return null;
    }

    // A new feature added to p161 
    // Return the maximal number of threads to be spawned in the system
    public int getMaxThreads( ) {
	return tids.length;
    }

    public Scheduler( ) {
    	timeSlice = DEFAULT_TIME_SLICE;
        // initialize the vector array
        queue = new Vector[3];
        // initialize each queue with a new vector 
    	for (int i = 0; i < 3; i++)
    	{
    	    queue[i] = new Vector();
    	}
    	initTid( DEFAULT_MAX_THREADS );
    }

    public Scheduler( int quantum ) {
    	timeSlice = quantum;
        queue = new Vector[3];
    	for (int i = 0; i < 3; i++)
        {
            queue[i] = new Vector();
        }
    	initTid( DEFAULT_MAX_THREADS );
    }

    // A new feature added to p161 
    // A constructor to receive the max number of threads to be spawned
    public Scheduler( int quantum, int maxThreads ) {
    	timeSlice = quantum;
        queue = new Vector[3];
    	for (int i = 0; i < 3; i++)
        {
            queue[i] = new Vector();
        }
    	initTid( maxThreads );
    }

    private void schedulerSleep(int milliseconds ) {
    	try {
    	    Thread.sleep( milliseconds );
    	} catch ( InterruptedException e ) {}
    }

    // A modified addThread of p161 example
    public TCB addThread( Thread t ) {
//	t.setPriority( 2 );
    	TCB parentTcb = getMyTcb( ); // get my TCB and find my TID
    	int pid = ( parentTcb != null ) ? parentTcb.getTid( ) : -1;
    	int tid = getNewTid( ); // get a new TID
    	if ( tid == -1)
    	    return null;
    	TCB tcb = new TCB( t, tid, pid ); // create a new TCB
    	queue[0].add( tcb );   // add a new thread to queue0
    	return tcb;
    }

    // A new feature added to p161
    // Removing the TCB of a terminating thread
    public boolean deleteThread( ) {
    	TCB tcb = getMyTcb( ); 
    	if ( tcb!= null )
    	    return tcb.setTerminated( );
    	else
    	    return false;
    }

    public void sleepThread( int milliseconds ) {
    	try {
    	    sleep( milliseconds );
    	} catch ( InterruptedException e ) { }
    }
    
    // process and execute all available threads in queue 0
    public void execQueue0()
    {
        while (queue[0].size() > 0)
        {
            try{
            TCB currentTCB = (TCB)queue[0].firstElement( );
            if ( currentTCB.getTerminated( ) == true ) {
                queue[0].remove( currentTCB );
                returnTid( currentTCB.getTid( ) );
                continue;
            }
            
            Thread current = currentTCB.getThread( );
            if ( current != null ) {
                if ( current.isAlive( ) )
                    current.resume();
                else {
                // Spawn must be controlled by Scheduler
                // Scheduler must start a new thread
                current.start( ); 
                }
            }
            
            // quantum for queue0 is 500ms
            schedulerSleep(timeSlice/2);
            
            synchronized ( queue[0] ) {
                if ( current != null && current.isAlive( ) )
                {
                  current.suspend();
                }
                queue[0].remove( currentTCB ); // remove unfinished thread from queue0
                queue[1].add( currentTCB ); // add unfinished thread to queue1
            } 
            
        }catch(NullPointerException e){};
        } 
    }
    
    // process all available threads from queue1
    public void execQueue1()
    {
        while (queue[1].size() > 0)
        {
            try{
            TCB currentTCB = (TCB)queue[1].firstElement( );
            if ( currentTCB.getTerminated( ) == true ) {
                queue[1].remove( currentTCB );
                returnTid( currentTCB.getTid( ) );
                continue;
            }

            Thread current = currentTCB.getThread( );
            if ( current != null ) {
                if ( current.isAlive( ) )
                    current.resume();
                else {
                // Spawn must be controlled by Scheduler
                // Scheduler must start a new thread
                current.start( ); 
                }
            }
            
            // process for 500ms first
            schedulerSleep(timeSlice/2);
            
            // check queue0, if it's not empty, call execQueue0 to process
            // all available threads in queue0
            if (queue[0].size() > 0)
            {
                // if the thread is alive, suspend it
                if ( current != null && current.isAlive( ) )
                {
                  current.suspend();
                }
                execQueue0();
                // resume the thread for the rest 500ms
                if ( current != null && current.isAlive( ) )
                {
                  current.resume();
                }
            }
            
            // continue processing the current thread for another 500ms
            schedulerSleep(timeSlice/2);
            
            synchronized ( queue[1] ) {
                if ( current != null && current.isAlive( ) )
                {
                  current.suspend();
                }
                queue[1].remove( currentTCB ); // remove unfinished thread from queue1
                queue[2].add( currentTCB ); // move thread to queue2
            } 
            }catch(NullPointerException e){};
        }
    }
    
    // process all available threads in queue2
    public void execQueue2()
    {
        while (queue[2].size() > 0)
        {
            try{
            TCB currentTCB = (TCB)queue[2].firstElement( );
            if ( currentTCB.getTerminated( ) == true ) {
                queue[2].remove( currentTCB );
                returnTid( currentTCB.getTid( ) );
                continue;
            }

            Thread current = currentTCB.getThread( );
            if ( current != null ) {
                if ( current.isAlive( ) )
                    current.resume();
                else {
                // Spawn must be controlled by Scheduler
                // Scheduler must start a new thread
                current.start( ); 
                }
            }
            
            for (int time = 0; time < timeSlice * 2; time += timeSlice/2)
            {
                // check queue0 and queue1 every 500ms 
                schedulerSleep(timeSlice/2);
                if (queue[0].size() != 0 || queue[1].size() != 0)
                {
                    if ( current != null && current.isAlive( ) )
                    {
                      current.suspend(); // suspend the current thread
                    }
                    execQueue0();   // execute all threads in queue0
                    execQueue1();   // execute all threads in queue1
                    
                    if ( current != null && current.isAlive( ) )
                    {
                      current.resume(); // resume the current threads
                    }
                }
            }
            
            synchronized ( queue[2] ) {
                if ( current != null && current.isAlive( ) )
                {
                  current.suspend();              
                }
                queue[2].remove( currentTCB ); // move the current thread to the tail
                queue[2].add( currentTCB );
            } 
            }catch(NullPointerException e){};
        }
    }
    
    // A modified run of p161
    public void run( ) {
        //	this.setPriority( 6 );
    	int runningTime = 0;
    	while ( true ) {
    	        execQueue0(); // empty queue0 
    	        execQueue1(); // empty queue1
    	        execQueue2(); // empty queue2
    	}
    }
}
