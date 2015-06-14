import java.util.Vector;

public class FileTable {
   // the actual entity of this file table
   private Vector<FileTableEntry> table; 
   // the root directory        
   private Directory dir; 
   // list from superblock        
   private Inode[] iList;       
   
   /*
    * constructor: initialize a file table
    */
   public FileTable( Directory directory, Inode[] iList ) { 
      table = new Vector<FileTableEntry>();    
      dir = directory;           // receive a reference to the Director
      this.iList = iList;  
   }                             
 
   /* 
    * allocate a new file (structure) table entry for this file name
    * allocate/retrieve and register the corresponding inode using dir
    * increment this inode's count
    * immediately write back this inode to the disk
    * return a reference to this file (structure) table entry
    */
   public synchronized FileTableEntry falloc( String filename, String mode ) {
	   short iNumber = -1;
	   Inode inode = null;
	   
	   // get i number from its filename, root equal 0
	   if (filename.equals( "/" )){
		   iNumber = 0;
	   }else{
		   iNumber = dir.namei( filename );
	   }
	   
	   // if file is found
	   if (iNumber >= 0){
		   inode = iList[iNumber];
	   }else{
		   // cannot find the file
		   // if not in read mode, can choose to add new file in it.
		   if( mode.compareTo("r") != 0 ) {
               iNumber = dir.ialloc(filename);
               // if cannot add new file, return null
               if( iNumber < 0 )
                  return null;
               
               // Create a new file and put its inode number into the list
               // get a new Inode
               inode = new Inode();
               iList[iNumber] = inode;
            } else {
               // if in the read mode, return null means cannot find the file	
               return null;               
            }
	   }
	   
	   // if the mode is write or append
	   if (mode.compareTo("r") != 0) {
		   
		   // wait until flag is not in read or write signal
		   while (inode.flag > 0) {
              try {
                 wait();
              } catch (InterruptedException e) {}
	       }
           
	   	   // increase the count means one more
	   	   // user in use
           inode.count++;
           inode.flag = 2; // set to the write signal	   
	   }else{
		   // in read mode 
		   // wait until flag is in write signal
		   while (inode.flag > 1) {
              try {
                 wait();
              } catch (InterruptedException e) {}
	       }		   
		   // increase the count means one more
	   	   // user in use
		   inode.count++;
           inode.flag = 1; // set to the read signal
	   }
	   	 
	    // update the inode into the disk
        inode.toDisk( iNumber );         
        // create a new entry
        FileTableEntry newEntry = new FileTableEntry( inode, iNumber, mode );
        table.addElement( newEntry );             // add a new entry to FileTable
        return newEntry;                          // return the new entry
 
   }
   
   /*
    * @parameter: a file table entry reference
    * save the corresponding inode to the disk
    * free this file table entry
    * @return: true if this file table entry is found in table
    */
   public synchronized boolean ffree( FileTableEntry e ) {
	   
	   // if there is no reference to this file, return false
	   if (e.inode.count == 0) {
           return false;
        }
       
        // if there is somebody use this entry, decrease one
        e.inode.count--;
        // Now, if nobody use it, set to the unused signal
        if (e.inode.count == 0) {
           e.inode.flag = 0;
           // save the corresponding inode to the disk
           notify();
        }
        // free it
        return table.removeElement(e);
   }
 
   /*
    * @return: if the table is empty
    * call it before formatting
    */
   public synchronized boolean fempty( ) {
      return table.isEmpty( );  
   }                          
}
