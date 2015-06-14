# Operating-System-Class-Work 
About all Operating System ThreadOS assignments (Java)

P1-System Call and Shell

1. Code a C++ program, named processes.cpp that receives one argument, (i.e., argv[1]) upon its invocation and searches how many processes
whose name is given in argv[1] are running on the system where your program has been invoked (fork a process)
2. Code Shell.java, a Java thread that will be invoked from ThreadOS Loader and behave as a shell command interpreter

P2-OS Scheduler

This assignment implements and compares two CPU scheduling algorithms, the round-robin scheduling and the multilevel feedback-queue scheduling

P3-Synchronization

In this assignment you will implement the monitors utilized by ThreadOS. While standard Java monitors are only able to wake up one (using notify)
or all (using notifyall()) sleeping threads, the ThreadOS monitors (implemented in SynchQueue.java) allows threads to sleep and wake up on a 
specific condition. These monitors are used to implement two seperate but key aspects of ThreadOS.

P4-Cache

This assignment focus on page replacement mechanisms and the performance improvements achieved by implementing a buffer cache that stores 
frequently-accessed disk blocks in memory. In this assignment you will implement the enhanced second-chance algorithm

FP-File System

The file system should provide user threads with the system calls that will allow them to format, to open, to read from , to write to,
to update the seek pointer of, to close, to delete, and to get the size of their files
