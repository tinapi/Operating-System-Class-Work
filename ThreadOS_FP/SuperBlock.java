public class SuperBlock
{
    public int totalBlocks; // the number of disk blocks
    public int totalInodes; // the number of inodes
    public int freeList;    // the block number of the free list's head
    
    /*
     * constructor: read the first block from disk
     * initialize total blocks, inodeblocks and freeList
     */
    public SuperBlock(int diskSize)
    {
        // create a byte array to read the first disk block
        byte firstBlock[] = new byte[Disk.blockSize];
        SysLib.rawread(0, firstBlock);
        
        // initialize three variables
        totalBlocks = SysLib.bytes2int(firstBlock, 0);
        totalInodes = SysLib.bytes2int(firstBlock, 4);
        freeList = SysLib.bytes2int(firstBlock, 8);
        
        // verify the three variables
        if (totalBlocks == diskSize && totalInodes > 0 && freeList > 2)
            return;
        
        // otherwise, format the disk first
        totalBlocks = diskSize;
        format(64);
        SysLib.cerr("DEFAULT DISK FORMAT\n");      
    }
    
    /*
     * Format the disk
     * @parameter files: maximum number of files to be created
     * @return 0: success, -1: error
     */
    public void format(int files)
    {
        // set the totalInodes
        totalInodes = files;
        
        // initialize each file
        for (int i = 0; i < files; i++)
        {
            Inode inode = new Inode();
            inode.flag = 0;     // set flag as unused
            inode.toDisk((short)i);    // write into disk
        }
        
        // calculate the block number of the free list's head
        freeList = totalInodes * 32 / Disk.blockSize + 2;
        
        // write bytes        
        for (int i = freeList; i < totalBlocks; i++)
        {
            byte[] data = new byte[Disk.blockSize];
            // empty each block
            for (int j = 0; j < Disk.blockSize; j++)
            {
                data[j] = 0;
            }
            
            SysLib.int2bytes(i + 1, data, 0);
            SysLib.rawwrite(i, data);
        }
        
        // sync superblock after formatting
        sync();
    }
    
    /*
     * Sync modified superblock: store back to first disk block
     */
    public void sync()
    {
        byte[] data = new byte[512];
        SysLib.int2bytes(totalBlocks, data, 0);
        SysLib.int2bytes(totalInodes, data, 4);
        SysLib.int2bytes(freeList, data, 8);
        SysLib.rawwrite(0, data);
        
        // notify the sync action
        SysLib.cerr("Superblock has synchronized!\n");
    }
    
    /*
     * Find the free block 
     */
    public int getFreeBlock()
    {
        int freeBlock = freeList;
        
        // if freeblock is -1, reutrn
        if (freeBlock == -1)
            return -1;
        
        byte data[] = new byte[512];
        SysLib.rawread(freeBlock, data);
        
        // modify the head
        freeList = SysLib.bytes2int(data, 0);
        SysLib.int2bytes(0, data, 0);
        SysLib.rawwrite(freeBlock, data);
               
        return freeBlock;
    }
    
    /*
     * add a new block to free list
     * set the freeList to new blockNum
     */
    public boolean returnBlock(int blockNum)
    {
        if (blockNum < 0)
            return false;
        
        byte data[] = new byte[512];
        // empty the block
        for (int i = 0; i < 512; i++)
            data[i] = 0;
        
        SysLib.int2bytes(freeList, data, 0);
        SysLib.rawwrite(blockNum, data);
        freeList = blockNum;
        
        return true;
    }
}