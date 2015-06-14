import java.util.Arrays;
import java.util.regex.Pattern;

/*
 * File system should provide user threads with the system calls that will 
 * allow them to format, to open, to read from , to write to, to update the 
 * seek pointer of, to close, to delete, and to get the size of their files.
 *
 * @author Jingjing Dong, Xueting Pi & Shifeng Wu
 */
public class FileSystem {

    private SuperBlock sb;
    private Directory dir;
    private FileTable ft;
    private Inode[] iList;
    
    //-------------------------------------Constructor-----------------------------      
    public FileSystem( int db ) {
    	// set the sb from disk block
    	sb = new SuperBlock(db);
    	dir = new Directory(sb.totalInodes);
    	iList = new Inode[sb.totalInodes];
        for (short i = 0; i < sb.totalInodes; i++) {
            iList[i] = new Inode(i);
        }
        
        // file table
        ft = new FileTable(dir, iList);
        
        // read the file
        FileTableEntry fte = open("/", "r");
        int fteSize = fsize(fte);
        
        if (fteSize > 0) {
            byte[] fteData = new byte[fteSize];
            read(fte, fteData);
            dir.bytes2directory(fteData);
        }
        close(fte);
    }
    
    
    //----------------------------------Open------------------------------------------
    // Opens a file with given name
    public FileTableEntry open( String filename, String mode ) {
    	// Set a entry to the file
        FileTableEntry openEntry = ft.falloc( filename, mode );
        
        // if the mode is write, check it can dealloccate block or not
        if ( mode.equals("w") ) {
        	// if there is not true, return cannot open the file
            if ( this.deallocAllBlocks(openEntry) != true ) {
                return null;
            }
        }
        return openEntry;
    }  
    
    
    //---------------------------------deallocAllBlocks-------------------------------------
    // Deallocates inodes and blocks.
    private boolean deallocAllBlocks( FileTableEntry entry ) {
        if (entry.inode.length <= 0 ) {
            return true;
        }
        
        for ( short blockId : entry.inode.freeDirectBlocks() ) {
        	// if invalid block id
            if ( blockId == -1 ) {
                break;
            }
            // return the block
            sb.returnBlock(blockId);
        }
        
        byte[] freeBlockData = entry.inode.unregisterIndexBlock();
        if ( freeBlockData != null ) {
            for ( int i = 0; i < freeBlockData.length; i += 2 ) {
                int freeBlock = (short) SysLib.bytes2short(freeBlockData, i);
                sb.returnBlock(freeBlock);
            }
        }
        return true;
    }
 
    
    //--------------------------------------------write---------------------------------------------
    // Writes the contents of buffer to the file indicated by the ftEnt
    public int write( FileTableEntry ftEnt, byte[] buffer ) {
        Pattern writeModes = Pattern.compile("\\Aw\\+?|a\\z");
        // don't write read
        if (ftEnt == null || !writeModes.matcher(ftEnt.mode).matches()) {
            return -1;
        }
    	
        Inode node = ftEnt.inode;

        byte[] currentBlock = new byte[Disk.blockSize];
        byte[] indexBlock = new byte[Disk.blockSize];
        if (node.indirect > 0) {
            // read in the indriect data if it exists
            SysLib.rawread(node.indirect, indexBlock);
        } else {
            byte b = -1;
            Arrays.fill(indexBlock, b);
        }
        // set target equal to the block seekPtr is in
        short target = node.findTargetBlock(ftEnt.seekPtr);
        int numIndirect = node.numIndirectBlocks();
        if ( target < 0 ) {
            // if target returns -1, allocate data
            target = (short) sb.getFreeBlock();
            if ( !node.registerDirectBlock(target) ) {
                SysLib.short2bytes(target, indexBlock, numIndirect * 2);
                numIndirect++;
            }
        } else {
            // if target is not -1, read the block
            SysLib.rawread(target, currentBlock);
        }
        // count
        int count = ftEnt.seekPtr % Disk.blockSize;
        for ( int i = 0; i < buffer.length; i++ ) {
            if ( count == Disk.blockSize ) {
                // if count has reached the end of disc, write the data out
                SysLib.rawwrite(target, currentBlock);
                target = node.findTargetBlock(ftEnt.seekPtr);
                if( target <= 0 ){
                    // reach the end of the allocated data
                	// find a new target block
                    target = (short) sb.getFreeBlock();
                    if ( !node.registerDirectBlock( (short) target) ) {
                        SysLib.short2bytes(target, indexBlock, numIndirect * 2);
                        numIndirect++;
                    }
                } else {
                    SysLib.rawread(target, currentBlock);
                }
                // reset the count
                count = 0;
            }
            // If stil writing to the current block
            currentBlock[count] = buffer[i];
            count++;
            ftEnt.seekPtr++;
        }
        SysLib.rawwrite(target, currentBlock);
        // If seekPtr passed the length of the file
        if ( ftEnt.seekPtr > node.length ) {
            node.length = ftEnt.seekPtr;
        }

        if ( numIndirect > 0 ) {
            if ( node.indirect < 0 ) {
                node.indirect = (short) sb.getFreeBlock();
            }
            SysLib.rawwrite(node.indirect, indexBlock);
        }

        return buffer.length;
    }
    
    
    //-------------------------------Sync-----------------------------------
    // Synchronize disk with superblock.
    public void sync() {
        FileTableEntry dirEnt = open( "/", "w" );
        byte[] dirData = dir.directory2bytes();
        // write the file table entry to th byte directory
        write(dirEnt, dirData);
        close(dirEnt);
        iList[0].toDisk((short) 0);
        // write to the disk
        dirEnt.inode.toDisk(dirEnt.iNumber);
    }

    //------------------------------Format------------------------------------
    // Formats the disk on give number of files
    public boolean format( int files ) {
        iList = new Inode[sb.totalInodes];
        for ( int i = 0; i < iList.length; i++ ) {
            iList[i] = new Inode();
        }
        // formate the sb
        sb.format(files);
        dir = new Directory(sb.totalInodes);
        ft = new FileTable(dir, iList);
        return true;
    }

    //-------------------------------Close-----------------------------------
    // Close the file corresponding to the file table entry.
    public boolean close( FileTableEntry ftEnt ) {
        // Closes the file in ftEnt
        return ft.ffree(ftEnt);
    }

    //-------------------------------fsize-----------------------------------
    // Get the size of the file in bytes
    public int fsize( FileTableEntry ftEnt ) {
        //Returns the length of ftEnt's file
        return ftEnt.inode.length;
    }

    //--------------------------------read----------------------------------
    // Reads  up to buffer length number of bytes from file indicated by FTE
    // Returns the number of bytes that's been read or error code if occurs
    public int read( FileTableEntry ftEnt, byte[] buffer ) {
        if (ftEnt == null) {
            return -1;
        }
        Inode node = ftEnt.inode; // Node we are reading
        byte[] currentBlock = new byte[Disk.blockSize];
        if ( node.findTargetBlock(ftEnt.seekPtr) > -1 ) {
            SysLib.rawread(node.findTargetBlock(ftEnt.seekPtr), currentBlock);
            for ( int i = 0; i < buffer.length; i++ ) {
                if ( ftEnt.seekPtr % Disk.blockSize == 0 ) {
                    // get the next block
                    if ( node.findTargetBlock(ftEnt.seekPtr) < 0 ) {
                        return -1;
                    }
                    SysLib.rawread(node.findTargetBlock(ftEnt.seekPtr), currentBlock);
                }
                // Set the buffer equal to the block and reset seekPtr
                buffer[i] = currentBlock[ftEnt.seekPtr % Disk.blockSize];
                ftEnt.seekPtr++;
            }
            // return amount we read
            return buffer.length;
        }
        return -1;
    }

    //----------------------------------seek--------------------------------
    // Update the seek pointer
    public int seek( FileTableEntry ftEnt, int offset, int whence ) {
    	// move the pointer through file depends on offset
        Inode node = ftEnt.inode;
        // SEEK_SET
        if ( whence == 0 ) {
            if ( offset >= 0 && offset < node.length ) {
                ftEnt.seekPtr = offset;
                return ftEnt.seekPtr;
            }
        // SEEK_CUR
        } else if ( whence == 1 ) {
            int newSeekPtr = ftEnt.seekPtr + offset;
            if ( 0 <= newSeekPtr && newSeekPtr < node.length ) {
                ftEnt.seekPtr = newSeekPtr;
                return newSeekPtr;
            }
        // SEEK_END
        } else if (whence == 2 ) {
            if ( offset < 0 && (-1 * offset) < node.length ) {
                ftEnt.seekPtr = node.length + offset;
                return ftEnt.seekPtr;
            }
        }
        return -1;
    }
    
    //-------------------------------delete-----------------------------------
    // Delete a file by given file name
    public boolean delete( String filename ) {
        int iNumber = this.dir.namei(filename);
        if (iNumber < 0) {
            return false;
        } else {
            return this.dir.ifree((short)iNumber);
        }
    }
    
}
