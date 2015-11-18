package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.*;

import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		/*int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPhysPages];
		for (int i = 0; i < numPhysPages; i++)
			pageTable[i] = new TranslationEntry(i, i, true, false, false, false);*/
		UserKernel.lock.acquire();
		processID = UserKernel.nextProcessID++;
		UserKernel.lock.release();
		files[openCount++] = UserKernel.console.openForReading();
		files[openCount++] = UserKernel.console.openForWriting();
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
		return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		//System.out.println("in execute");
		if (!load(name, args))
			return false;

		UserKernel.activeProcess++;
		cT = new UThread(this).setName(name);
		cT.fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr the starting virtual address of the null-terminated string.
	 * @param maxLength the maximum number of characters in the string, not
	 * including the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 * found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @param offset the first byte to write in the array.
	 * @param length the number of bytes to transfer from virtual memory to the
	 * array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		//if (vaddr < 0 || vaddr >= memory.length)
		//	return 0;
		
		/*int rvaddr = -1;
		for(int i = 0; i < numPages; i++){
			if(pageTable[i].vpn == vaddr){
				rvaddr = i;
				break;
			}
		}

		if(rvaddr == -1)
			return 0;*/
		//System.out.println("vaddr in rvm is: " + vaddr);
		int pageID = Processor.pageFromAddress(vaddr);
		//System.out.println("pageID in rvm is: " + pageID);
		if(pageID >= numPages)
			return 0;
		TranslationEntry tmp = pageTable[pageID];
		//System.out.println("ppn in rvm is: " + tmp.ppn);
		if(!tmp.valid)
			return 0;
		int pageOffset = Processor.offsetFromAddress(vaddr);
		int paddr = tmp.ppn*Processor.pageSize + pageOffset; 

		if(paddr < 0 || paddr >= memory.length)
			return 0;

		int amount = Math.min(length, memory.length - paddr);
		System.arraycopy(memory, paddr, data, offset, amount);

		return amount;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @param offset the first byte to transfer from the array.
	 * @param length the number of bytes to transfer from the array to virtual
	 * memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		UserKernel.lock.acquire();
		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		//if (vaddr < 0 || vaddr >= memory.length)
		//	return 0;

		int pageID = Processor.pageFromAddress(vaddr);
		if(pageID >= numPages)
			return 0;

		TranslationEntry tmp = pageTable[pageID];
		if(tmp.readOnly || !tmp.valid)
			return 0;

		int pageOffset = Processor.offsetFromAddress(vaddr);
		int paddr = tmp.ppn*Processor.pageSize + pageOffset;

		if(paddr < 0 || paddr >= memory.length)
			return 0;

		int amount = Math.min(length, memory.length - paddr);
		System.arraycopy(data, offset, memory, paddr, amount);
		UserKernel.lock.release();

		return amount;
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			//System.out.println("open failed");
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		}
		catch (EOFException e) {
			executable.close();
			//System.out.println("coff load failed");
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				//System.out.println("fragmented executable");
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			//System.out.println("arguments too long");
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections()){
			//System.out.println("loadsection falt");
			return false;
		}

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		
		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			//UserKernel.lock.release();
			return false;
		}

		if(numPages > UserKernel.freePhysicalPages.size()){
			coff.close();
			Lib.debug(dbgProcess, "\t not enough free spaces");
			//UserKernel.lock.release();
			return false;
		}
		pageTable = new TranslationEntry[numPages];
		for(int i = 0; i < numPages; i++){
			pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
		}

		
		// load sections
		//System.out.println("num page is: " + numPages);
		//System.out.println("num section is: " + coff.getNumSections());

		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
		//System.out.println("length section is: " + section.getLength());
			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;
				
				UserKernel.lock.acquire();
				for(int j = 0; j < numPages; j++){
					if(!pageTable[j].used){
						//System.out.println("vpn is: " + vpn);
						pageTable[j].vpn = vpn;
						pageTable[j].readOnly = section.isReadOnly();
						pageTable[j].used = true;
						pageTable[j].ppn = UserKernel.freePhysicalPages.removeLast();
						//System.out.println("ppn is: " + pageTable[j].ppn);
						section.loadPage(i, pageTable[j].ppn);
						break;
					}
				}
				UserKernel.lock.release();
			}
		}

		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		UserKernel.lock.acquire();
		for(int i = 0; i < numPages; i++){
			if(pageTable[i] != null){
				UserKernel.freePhysicalPages.add(pageTable[i].ppn);
				pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
			}
		}
		UserKernel.lock.release();
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {

		if(UserKernel.activeProcess == 0)
			Machine.halt();

		//Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}

	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall the syscall number.
	 * @param a0 the first syscall argument.
	 * @param a1 the second syscall argument.
	 * @param a2 the third syscall argument.
	 * @param a3 the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallHalt:
			return handleHalt();
		case syscallExec:
			return handleExec(a0, a1, a2);
		case syscallJoin:
			return handleJoin(a0, a1);
		case syscallExit:
			handleExit(a0);
			return syscallExit;
		case syscallCreate:
			return handleCreate(a0);
		case syscallOpen:
			return handleOpen(a0);
		case syscallRead:
			return handleRead(a0, a1, a2);
		case syscallWrite:
			return handleWrite(a0, a1, a2);
		case syscallClose:
			return handleClose(a0);
		case syscallUnlink:
			return handleUnlink(a0);

		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!");
		}
		return 0;
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0),
					processor.readRegister(Processor.regA1),
					processor.readRegister(Processor.regA2),
					processor.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;

		default:
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
			Lib.assertNotReached("Unexpected exception");
		}
	}

	private void handleExit(int status){
		//System.out.println("in exit");
		if(coff != null)
			coff.close();

		for(int i = 0; i < files.length; i++){
			if(files[i]!=null){
				handleClose(i);
			}
		}

		for(int i = 0; i < childProcess.size(); i++){
			childProcess.get(i).parentProcess = null;
		}

		if(parentProcess != null)
			parentProcess.childProcess.remove(this);

		unloadSections();
		UserKernel.activeProcess--;
		//System.out.println("activeProcess is: " + UserKernel.activeProcess);
		
		if(UserKernel.activeProcess == 0){
			//System.out.println("exit kernel");
			UserKernel.kernel.terminate();
		}
		else{
			//System.out.println("exit kthread");
			cT.finish();
		}

		status = 0;
		this.status = 0;
		good = true;

	}

	private int handleJoin(int processID, int status){
		UserProcess child = null;

		for(int i = 0; i < childProcess.size(); i++){
			if(childProcess.get(i).processID == processID){
				child = childProcess.get(i);
				childProcess.remove(i);
				break;
			}
		}

		if(child == null || child.cT == null)
			return -1;
		child.cT.join();
		if(child.good != true)
			return 0;

		return 1;
	}


	private int handleExec(int localFile, int localArgc, int localArgv){
		//System.out.println("in handlexec");
		if(localArgc < 0)
			return -1;

		String fileName = readVirtualMemoryString(localFile, maxStringLength);

		System.out.println("fileName: " + fileName);	

		if(!fileName.contains(".coff"))
			return -1;


		String[] args = new String[localArgc];
		
		for(int i = 0; i < localArgc; i++){	
			byte[] bytes = new byte[4];
			readVirtualMemory(localArgv + i*4, bytes);
			int addr = Lib.bytesToInt(bytes, 0);
			args[i] = readVirtualMemoryString(addr, maxStringLength);
			//System.out.println("args is: " + args[i]);
		}

		UserProcess tmp = newUserProcess();

		UserKernel.processLock.acquire();
		int childProcessID = UserKernel.nextProcessID++;
		UserKernel.processLock.release();
		//System.out.println("childProcessID is: " + childProcessID);

		tmp.parentProcess = this;

		if(tmp.execute(fileName, args)){
			//System.out.println("successfully exit exec");
			return childProcessID;
		}
		//System.out.println("failed exit exec");
		return -1;

	}


	private int handleUnlink(int name){
		String fileName = readVirtualMemoryString(name, maxStringLength);
		boolean opened = false;
		int fd = -1;
		for(int i = 0; i < 16; i++){
			if(files[i].getName().equals(fileName)){
				opened = true;
				fd = i;
			}
		}

		if(opened == false)
			sfs.remove(fileName);


		return 0;

	}


	private int handleClose(int fd){
		if(fd > maxOpenCount || fd < 0)
			return -1;
		OpenFile closedFile = files[fd];
		if(files[fd] == null)
			return -1;
		files[fd] = null;
		openCount--;
		closedFile.close();
		return 0;
	}


	public int handleRead(int fd, int bva, int count){
		if(fd < 0 || fd >= 16)
			return -1;

		if(count < 0)
			return -1;

		OpenFile readFile = files[fd];

		if(readFile == null)
			return -1;

		byte[] bufferBytes = new byte[count];
		int numberBytes = readFile.read(bufferBytes, 0, count);
		if(numberBytes == -1)
			return -1;

		int check = writeVirtualMemory(bva, bufferBytes, 0, count);
		if(check == 0)
			return -1;

		return numberBytes;
	}

	public int handleWrite(int fd, int bva, int count){
	//System.out.println("fd is: " + fd);
		if(fd < 0 || fd >= 16)
			return -1;
//System.out.println("count is: " + count);
		if(count < 0)
			return -1;

		OpenFile writeFile = files[fd];
	//System.out.println(writeFile == null);
		if(writeFile == null)
			return -1;

		byte[] bufferBytes = new byte[count];
		int numberBytes = readVirtualMemory(bva, bufferBytes, 0, count);
	//System.out.println("number bytes is: " + numberBytes);

	//System.out.println("count is: " + count);
		if(numberBytes == 0 && count != 0)
			return -1;

		if(numberBytes != count)
			return -1;
		int check = writeFile.write(bufferBytes, 0, count);

	//System.out.println("check is: " + check);
		if(check == -1)
			return -1;

		return numberBytes;
	}

	/**
	 *open a existing file with the given name
	 * @param name the given name of the file opened
	 * @return success = file access point, fail = -1;
	 */ 

	public int handleOpen(int name){
		String fname = readVirtualMemoryString(name, maxStringLength);
		int accessPoint = openCount;
		OpenFile tmp = sfs.open(fname, false);
		if(tmp == null)
			return -1;
		if(openCount < maxOpenCount)
			files[openCount++] = tmp;
		else
			System.out.println("open too much file!");
		return accessPoint;
	}

	/**
	 *create a new file with the given name
	 * @param name the given name of the file created
	 * @return success = file access point, fail = -1;
	 */ 
	public int handleCreate(int name){
		String fname = readVirtualMemoryString(name, maxStringLength);
		OpenFile nfile = sfs.open(fname, false);
		int accessPoint = openCount;
		if(nfile == null){
			nfile = sfs.open(fname, true);
		}
		if(openCount < maxOpenCount)
			files[openCount++] = nfile;
		else
			System.out.println("open too much file!");
		return accessPoint;
	}

	/*need to be implemented*/
	/*public int handleExit(int status){
		return 0;
	}*/

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;

	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	private int initialPC, initialSP;

	private int argc, argv;

	private int openCount = 0;

	private int maxOpenCount = 16;

	private int maxStringLength = 256;

	private OpenFile[] files = new OpenFile[16];

	private FileSystem sfs= ThreadedKernel.fileSystem;

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	public int processID = 0;
	public LinkedList<UserProcess> childProcess = new LinkedList<UserProcess>();
	public UserProcess parentProcess = null;
	public boolean good = false;
	public int status = -1;
	public KThread cT;
}
