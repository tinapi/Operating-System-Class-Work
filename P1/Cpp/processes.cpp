/*
author: Xueting Pi
CSS 430 HW1
4/15/2015
*/
#include <stdlib.h>     // for exit
#include <stdio.h>      // for perror
#include <unistd.h>     // for fork, pipe
#include <sys/wait.h>   // for wait
#include <iostream>     // for cerr, cout
#include <string>
#define MAXSIZE 4096 
using namespace std;


int main( int argc, char* argv[] ) {
   enum {RD, WR}; // pipe fd index RD=0, WR=1
   int pipe1_fd[2], pipe2_fd[2];
   pid_t pid;

   // argument not enough
   if (argc < 2)
      perror("not enough argument");
   
   // check pipe errors
   if( pipe(pipe1_fd) < 0 || pipe(pipe2_fd) < 0) 
      perror("pipe error");
   

   switch(fork()) {
      case -1:
         perror("fork error");

      // child ---- wc -l
      // ONLY ALLOWED TO READ FROM PIPE2
      case 0:
      {
         close(pipe2_fd[WR]);
         close(pipe1_fd[WR]);
         dup2(pipe2_fd[RD],0);
         close(pipe2_fd[RD]);
         execlp("wc","wc","-l", NULL);
      }

      // fork a grandchild
      default:
      {
         // create grandchild process
         switch(fork())
         {
            case -1:
               perror("fork error");
            // grand child -------- grep <uwnetid> 
            // READ FROM PIPE1 AND WRITE TO PIPE2
            case 0:
            {
               close(pipe2_fd[RD]);
               close(pipe1_fd[WR]);
               dup2(pipe1_fd[RD],0);
               dup2(pipe2_fd[WR],1);

               // close the two open fds
               close(pipe1_fd[RD]);
               close(pipe2_fd[WR]);

               execlp("grep", "grep", argv[1], NULL);
            }
            default:
            {
               // create great grandchild process
               switch(fork())
               {
                  case -1:
                     perror("fork error");
                  // great grand child --------- ps -A
                  // close pipe2 read and open write
                  case 0:
                  {
                     close(pipe1_fd[RD]);
                     close(pipe2_fd[WR]);
                     dup2(pipe1_fd[WR],1);
                     close(pipe1_fd[WR]);
                     execlp("ps","ps","-A", NULL);
                     
                  }
                  default:
                  {
                     wait( NULL );  
                     cout << "Parent Done!" << endl;
                     exit(EXIT_SUCCESS);
                  }
               }             
            }
         }          
         // Never returns here!!
         
      }
   }
}



