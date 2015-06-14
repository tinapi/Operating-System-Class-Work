public class Directory
{
	// max characters of each file name
	private static int maxChars = 30;	

	// directory entries
	private int fsize[];	// each element stores a different file size
	private char fnames[][];	// each element stores a different file name

	/*
	 * constructor: initialize max files
	 */
	public Directory(int maxInumber)
	{
		fsize = new int[maxInumber];
		// empty the file size array
		for (int i = 0; i < maxInumber; i++)
			fsize[i] = 0; 

		fnames = new char[maxInumber][maxChars];
		String root = "/";	// entry 0 is "/"
		fsize[0] = root.length();
		root.getChars(0, fsize[0], fnames[0], 0);
	}

	/*
	 * assumes data[] received directory info from disk
	 * initialize the directory instance with this data[]
	 */
	public void bytes2directory(byte data[])
	{
		int offset = 0;

		// convert data from bytes
		for (int i = 0; i < fsize.length; i++)
		{
			fsize[i] = SysLib.bytes2int(data, offset);
			offset += 4;
		}

		// populate file names
		for (int i = 0; i < fnames.length; i++)
		{
			String s = new String(data, offset, maxChars * 2);
			if (fsize[i] < maxChars)
				s.getChars(0, fsize[i], fnames[i], 0);
			offset += maxChars * 2;
		}
	}

	/*
	 * convert and return Directory info into a byte array
	 * this byte array will be written back to disk
	 */
	public byte[] directory2bytes()
	{
		// int size: 32 bits/ 4 bytes
		// char size: 16 bits/ 2 bytes
		byte data[] = new byte[fsize.length*4 + fnames.length*2*maxChars];

		int offset = 0;

		// convert size to byte
		for (int i = 0; i < fsize.length; i++)
		{
			SysLib.int2bytes(fsize[i], data, offset);
			offset += 4;
		}

		// convert file name to byte
		for (int j = 0; j < fnames.length; j++)
		{
			String s = new String(fnames[j], 0, fsize[j]);
			byte bytes[] = s.getBytes();
			System.arraycopy(bytes, 0, data, offset, bytes.length);
			offset += maxChars * 2;
		}

		return data;
	}

	/*
	 * filename is the file to be created
	 * allocate a new inode number for this filename
	 */
	public short ialloc(String fileName)
	{
		for (short i = 0; i < fsize.length; i++)
		{
			// find an empty file entry
			if (fsize[i] == 0)
			{
				// set the file size
				fsize[i] = fileName.length() < maxChars? (short)fileName.length() : maxChars;
				fileName.getChars(0, fsize[i], fnames[i], 0);
				return i;
			}
		}
		return (short)-1;
	}

	/*
	 * deallocate this inode number
	 * delete the corresponding file
	 */
	public boolean ifree(short iNumber)
	{
		// if inode number is invalid
		if (iNumber > fsize.length-1 || iNumber < 0)
			return false;
		fsize[iNumber] = 0;
		return true;
	}

	/*
	 * return the inumber corresponding to this filename
	 */
	public short namei(String filename)
	{
		// traverse through to find the filename
		for (int i = 0; i < fsize.length; i++)
		{
			String s = new String(fnames[i], 0, fsize[i]);
			if (filename.equals(s))
				return (short)i;
		}

		return (short)-1;
	}
}