/*
 * This Inode class describes one file which keeps track of index blocks
 * containing both direct and in direct as well as length of file, count
 * of file-table entires, and status of the file.
 *
 * Written by: Jingjing Dong, Xueting Pi, & Shifeng Wu
 * Date: 06/09/2010
 */

public class Inode {
	
   // Inode has static size which is 32 byte
   private final static int iNodeSize = 32; 
   
   // And static directly size is 11 
   private final static int directSize = 11;     
   
   
   // the state of flag
   private final static int UNUSED = 0;
   private final static int USED = 1;
   private final static int WRITE = 2;
   
   public int length;                             // file size in bytes
   public short count;                            // # file-table entries pointing to this
   public short flag;                             // 0 = unused, 1 = used, 2 = write
   public short direct[] = new short[directSize]; // set the direct data size
   public short indirect;                         // set the pointer to point the indirect data

//------------------------------------constructor------------------------------------------------
   public Inode() {                                     
      length = 0;
      count = 0;
      flag = 0;
      for ( int i = 0; i < directSize; i++ )
         direct[i] = -1;
      indirect = -1;
   }

   
//------------------------------------inode-------------------------------------------------------
// receive inode from disk   
   public Inode( short iNumber ) {                      
      int blkNumber = iNumber / 16 + 1;
      // load the block
      byte[] data = new byte[Disk.blockSize]; 
      SysLib.rawread( blkNumber, data );
      // read offset from disk
      int offset = ( iNumber % 16 ) * iNodeSize; 
    
      // header part
      length = SysLib.bytes2int( data, offset );
      offset += 4;
      // counter part
      count = SysLib.bytes2short( data, offset );
      offset += 2;
      // flag part
      flag = SysLib.bytes2short( data, offset );
      offset += 2;
      // direct data
      for ( int i = 0; i < directSize; i++ ) {
         direct[i] = SysLib.bytes2short( data, offset );
         offset += 2;
      }
      // indirect pointer
      indirect = SysLib.bytes2short( data, offset );
   }

   
//--------------------------------------toDisk----------------------------------------------
// save inode into the disk
   public void toDisk( short iNumber ) {                 

      int target = (iNumber / 16) + 1;
      // reserve space to write
      byte[] temp = new byte[Disk.blockSize];
      // read the offset from data
      int offset = (iNumber * iNodeSize) % Disk.blockSize;
      // write the length into disk
      SysLib.int2bytes( length, temp, offset );
      offset += 4;
      // write the counter into disk
      SysLib.short2bytes( count, temp, offset );
      offset += 2;
      // write the flag into disk
      SysLib.short2bytes( flag, temp, offset );
      offset += 2;
      // write the direct data into disk
      for( int i = 0; i < directSize; i++ ) {
         SysLib.short2bytes( direct[i], temp, offset );
         offset += 2;
      }
      // add indirect pointer into disk
      SysLib.short2bytes( indirect, temp, offset );

      // write the whole block into disk
      SysLib.rawwrite( target, temp );
   }

 //-------------------------------------findIndexBlock---------------------------------------------
   public int findIndexBlock( ) {
      return indirect;
   }

 //--------------------------------- registerIndexBlock---------------------------------------------
   public boolean registerIndexBlock( short indexBlockNumber ) {
      // check is the block have index or not
      if( findIndexBlock() == -1 ) {
         // if it does not have index, give it an index number
         indirect = indexBlockNumber;
         return true;
      } else {
         // index block already exist, so don't assigned it
         return false;
      }
   }

 //--------------------------------- UnregisterIndexBlock--------------------------------------------- 
   public byte[] unregisterIndexBlock( ) {
      // check the block is exist or not
      if( indirect >= 0 ) {
    	  // if indirect block is exist
         byte[] temp = new byte[Disk.blockSize];
         SysLib.rawread( indirect, temp );
         indirect = -1;
         return temp;
      } else {
         return null;
      }
   }

//--------------------------------- registerTargetBlock--------------------------------------------- 
   public boolean registerTargetBlock( short freeBlock ) {
      // search which inode fits the requirement
      for (int i = 0; i < direct.length; i++) {
         if (direct[i] == -1) {
             direct[i] = freeBlock;
             return true;
         }
      }
      return false;
   }

 //--------------------------------- findTargetBlock---------------------------------------------
   public short findTargetBlock(int offset) {
      // search the inode
      int target = offset / Disk.blockSize;
      if ( target < direct.length ) {
         // direct pointer
         return direct[target];
      } else {
         // indirect pointer
         if (indirect < 0) {
            // if there is no block exist, return -1
            return -1;
         }

         // if there is the indirect pointer, get from disk
         byte[] indirectBlock = new byte[Disk.blockSize];
         SysLib.rawread(indirect, indirectBlock);
         // return indirect pointer
         return SysLib.bytes2short(indirectBlock, (target - direct.length) * 2);
      }
   }

//---------------------------freeDirectBlock--------------------------------------------------
   public short[] freeDirectBlocks() {
      // clear all direct block
      short[] temp = new short[directSize];
      for (int i = 0; i < directSize; i++) {
         temp[i] = direct[i];
         direct[i] = -1;
      }
      return temp;
   }

//---------------------------registerDirectBlock-----------------------------------------------
   public boolean registerDirectBlock(short freeBlock) {
      // register direct block
      for (int i = 0; i < direct.length; i++) {
         if (direct[i] == -1) {
             direct[i] = freeBlock;
             return true;
         }
      }
      // if there is all full, return false
      return false;
   }

 //---------------------------numIndirectBlocks-----------------------------------------------
   public final int numIndirectBlocks() {
      // return the number of indirect blocks
      int numIndirectBlocks = length / Disk.blockSize - direct.length;
      if (numIndirectBlocks < 0) {
         return 0;
      }
      return numIndirectBlocks;
   }
}
