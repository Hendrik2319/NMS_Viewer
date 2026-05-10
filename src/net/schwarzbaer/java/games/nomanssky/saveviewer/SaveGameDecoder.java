package net.schwarzbaer.java.games.nomanssky.saveviewer;

import java.awt.Dialog.ModalityType;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.schwarzbaer.java.games.nomanssky.saveviewer.SaveGameData.FactoryForExtras;
import net.schwarzbaer.java.lib.gui.StandardMainWindow;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser;

class SaveGameDecoder
{
	static final int VERBOSE_LEVEL_Nothing = 0;
	static final int VERBOSE_LEVEL_Files_Only = 1;
	static final int VERBOSE_LEVEL_Blocks_Only = 2;
	static final int VERBOSE_LEVEL_Blocks_and_Sequences = 3;

	public static void main(String[] args)
	{
		Tester.processCommandLineArgs(args);
	}

	static byte[] decodeFile(File file)
	{
		return decodeFile(file, VERBOSE_LEVEL_Nothing);
	}

	static byte[] decodeFile(File file, int verboseLevel)
	{
		if (!file.isFile()) return null;
		
		if (verboseLevel>=VERBOSE_LEVEL_Blocks_Only)
			Gui.log_ln("Read file \"%s\" ...", file.getAbsolutePath());
		else if (verboseLevel==VERBOSE_LEVEL_Files_Only)
			Gui.log("Read file \"%s\" ...", file.getAbsolutePath());
		
		byte[] bytes;
		try { bytes = Files.readAllBytes(file.toPath()); }
		catch (IOException e)
		{
			Gui.log_error_ln("%s occured while reading file \"%s\" : %s", e.getClass().getCanonicalName(), file.getAbsolutePath(), e.getMessage());
			// e.printStackTrace();
			return null;
		}
		
		if (bytes.length == 0)
		{
			if (verboseLevel>=VERBOSE_LEVEL_Blocks_Only)
				Gui.log_ln("   is empty");
			else if (verboseLevel==VERBOSE_LEVEL_Files_Only)
				Gui.log(" is empty");
		}
		else if ((bytes[0] & 0xFF) == '{')
		{
			if (verboseLevel>=VERBOSE_LEVEL_Blocks_Only)
				Gui.log_ln("   is plain text");
			else if (verboseLevel==VERBOSE_LEVEL_Files_Only)
				Gui.log(" is plain text");
		}
		else
		{
			if (verboseLevel==VERBOSE_LEVEL_Files_Only)
				Gui.log(" is encoded");
			bytes = decodeFileContent(bytes, file.getAbsolutePath(), verboseLevel);
		}
		
		if (verboseLevel>=VERBOSE_LEVEL_Blocks_Only)
			Gui.log_ln( "... done (%d bytes)", bytes.length);
		else if (verboseLevel==VERBOSE_LEVEL_Files_Only)
			Gui.log_ln(" ... done (%d bytes)", bytes.length);
		
		return bytes;
	}

	private static byte[] decodeFileContent(byte[] bytes, String filepath, int verboseLevel)
	{
		RawBytesReader in = new RawBytesReader(bytes);
		Output out = new Output(new OutputContext() {
			@Override public int getReadPos() { return in.getPos(); }
		});
		boolean showLiterals = false;
		
		try
		{
			int blockIndex = 0;
			while (!in.isEOF())
			{
				out.resetBuffer();
				if (verboseLevel>=VERBOSE_LEVEL_Blocks_and_Sequences)
					Gui.log_ln("Blocks[%d]", ++blockIndex);
				
				int magicNumber = in.readFourByte();
				if (magicNumber!=0xFEEDA1E5)
					throw new FormatException("wrong magic number (0x%X found, 0xFEEDA1E5 expected)", magicNumber);
				
				int compressedSize   = in.readFourByte();
				int uncompressedSize = in.readFourByte();
				int reserved         = in.readFourByte();
				if (verboseLevel>=VERBOSE_LEVEL_Blocks_and_Sequences)
				{
					Gui.log_ln("    magic number     : %X (%d)", magicNumber, magicNumber);
					Gui.log_ln("    uncompressed size: %X (%d)", uncompressedSize, uncompressedSize);
					Gui.log_ln("    compressed size  : %X (%d)", compressedSize, compressedSize);
					Gui.log_ln("    reserved size    : %X (%d)", reserved, reserved);
				}
				else if (verboseLevel==VERBOSE_LEVEL_Blocks_Only)
					Gui.log("    Blocks[%03d] { magic:OK, encoded size: %d, decoded size: %d }", ++blockIndex, compressedSize, uncompressedSize);
				
				int pos = in.getPos();
				int blockEnd = pos + compressedSize;
				
				while (pos < blockEnd)
				{
					readSequence(in, out, blockEnd, verboseLevel, showLiterals);
					pos = in.getPos();
				}
				
				if (pos > blockEnd)
					throw new FormatException("Unexpected stream position after last sequence of block: current pos: %d; expected pos: %d", pos, blockEnd);
				
				if (verboseLevel==VERBOSE_LEVEL_Blocks_Only)
					Gui.log_ln("  [out.pos:%016X]", out.out.size());
			}
		}
		catch (ReadException | FormatException ex)
		{
			Gui.log_error_ln("%s occured while uncompressing file \"%s\" : %s", ex.getClass().getCanonicalName(), filepath, ex.getMessage());
			// ex.printStackTrace();
			return null;
		}
		
		return out.getAllBytes();
	}
	
	private static void readSequence(RawBytesReader in, Output out, int blockEnd, int verboseLevel, boolean showLiterals) throws ReadException, FormatException
	{
		int pos = in.getPos();
		int totalSize = in.getTotalSize();
		int token = in.readOneByte();
		int literalsCount0 = (token & 0xF0) >> 4;
		int matchLength0   = (token & 0x0F) >> 0;
		if (verboseLevel>=VERBOSE_LEVEL_Blocks_and_Sequences)
		{
			Gui.log("    [%016X,%5.1f%%] Sequence {", pos, pos*100.0/totalSize);
			Gui.log(" token:0x%02X (lit0:%2d,mat0:%2d)", token, literalsCount0, matchLength0);
		}
		
		int literalsCount = readVarLength(in, literalsCount0);
		if (verboseLevel>=VERBOSE_LEVEL_Blocks_and_Sequences)
			Gui.log(", lit:0x%02X(%3d)", literalsCount, literalsCount);
		
		if (showLiterals) Gui.log(", lit[]:");
		out.copyLiterals(in,literalsCount,showLiterals);
		
		pos = in.getPos();
		if (pos < blockEnd)
		{
			int offset = in.readTwoByte();
			if (offset==0)
				throw new FormatException("Wrong offset: Offset==0 (InputReadPos:%d)", in.getPos());
			
			int matchLength = readVarLength(in, matchLength0) + 4;
			if (verboseLevel>=VERBOSE_LEVEL_Blocks_and_Sequences)
			{
				Gui.log(", offset:0x%04X(%2d)", offset, offset);
				Gui.log(", mat:0x%02X(%3d)", matchLength, matchLength);
			}

			if (showLiterals) Gui.log(", mat[]:");
			out.copyMatch(offset,matchLength,showLiterals);
		}
		else
		{
			if (pos > blockEnd)
				throw new FormatException("Unexpected stream position after last sequence of block: current pos: %d; expected pos: %d", pos, blockEnd);
			if (matchLength0!=0)
				throw new FormatException("Unexpected matchLength in last sequence of block: found: %d; expected: 0", matchLength0);
		}
		
		if (verboseLevel>=VERBOSE_LEVEL_Blocks_and_Sequences)
			Gui.log_ln(" } [out.pos:%016X]", out.out.size());
	}

	private static int readVarLength(RawBytesReader in, int val0) throws ReadException
	{
		int val = val0;
		if (val0==15)
		{
			int extra = in.readOneByte();
			while (extra==255)
			{
				val += 255;
				extra = in.readOneByte();
			}
			val += extra;
		}
		return val;
	}
	
	private interface OutputContext
	{
		int getReadPos();
	}
	
	private static class Output
	{
		private final OutputContext context;
		private final ByteArrayOutputStream out;
		private final byte[] buffer;
		private int writePos;
		private boolean firstRun;

		Output(OutputContext context)
		{
			this.context = context;
			out = new ByteArrayOutputStream();
			buffer = new byte[0x10000];
			resetBuffer();
		}

		void resetBuffer()
		{
			firstRun = true;
			writePos = 0;
		}

		byte[] getAllBytes()
		{
			return out.toByteArray();
		}

		void copyLiterals(RawBytesReader in, int literalsCount, boolean showLiterals) throws ReadException, FormatException
		{
			copyBytes(literalsCount, ()->in.readOneByte(), showLiterals);
		}

		void copyMatch(int offset, int matchLength, boolean showLiterals) throws ReadException, FormatException
		{
			copyBytes(matchLength, ()->getByteFromBuffer(offset), showLiterals);
		}
		
		private interface ReadByte
		{
			int readByte() throws ReadException, FormatException;
		}

		void copyBytes(int length, ReadByte getByte, boolean showLiterals) throws ReadException, FormatException
		{
			for (int i=0; i<length; i++)
			{
				int b = getByte.readByte();
				if (showLiterals) Gui.log(" %02X", b);
				addByte(b);
			}
		}

		private int getByteFromBuffer(int offset) throws FormatException
		{
			if (offset==0)
				throw new FormatException("Wrong offset: Offset==0 (InputReadPos:%d)", context.getReadPos());
			
			int index = writePos - offset;
			
			if (firstRun && index<0)
				throw new FormatException("Wrong offset: Offset (%d) points into range (%d) of unfilled buffer. (BufferWritePos:%d, InputReadPos:%d)", offset, index, writePos, context.getReadPos());
			
			while (index<0)
				index += buffer.length;
			
			return buffer[index];
		}

		private void addByte(int b)
		{
			out.write(b);
			buffer[writePos++] = (byte) b;
			
			while (writePos >= buffer.length)
			{
				firstRun = false;
				writePos -= buffer.length;
			}
		}
	}

	private static class RawBytesReader
	{
		private final byte[] bytes;
		private int readPos;

		RawBytesReader(byte[] bytes)
		{
			this.bytes = Objects.requireNonNull( bytes );
			readPos = 0;
		}

		int getTotalSize() { return            bytes.length; }
		boolean    isEOF() { return readPos >= bytes.length; }
		int       getPos() { return readPos                ; }

		int readOneByte() throws ReadException
		{
			if (readPos+1 > bytes.length)
				throw new ReadException("reading 1 byte goes beyond EndOfFile: readPos:%d, bytes.length:%d", readPos, bytes.length);
			
			return bytes[readPos++] & 0xFF;
		}

		int readTwoByte() throws ReadException
		{
			if (readPos+2 > bytes.length)
				throw new ReadException("reading 2 bytes goes beyond EndOfFile: readPos:%d, bytes.length:%d", readPos, bytes.length);
			
			return
					((bytes[readPos++] & 0xFF) <<  0) |
					((bytes[readPos++] & 0xFF) <<  8);
		}

		int readFourByte() throws ReadException
		{
			if (readPos+4 > bytes.length)
				throw new ReadException("reading 4 bytes goes beyond EndOfFile: readPos:%d, bytes.length:%d", readPos, bytes.length);
			
			return
					((bytes[readPos++] & 0xFF) <<  0) |
					((bytes[readPos++] & 0xFF) <<  8) |
					((bytes[readPos++] & 0xFF) << 16) |
					((bytes[readPos++] & 0xFF) << 24);
		}

		@SuppressWarnings("unused")
		void skip(int n) throws ReadException
		{
			if (readPos+n > bytes.length)
				throw new ReadException("reached EndOfFile");
			
			readPos += n;
		}
	}
	
	private static class ReadException extends Exception
	{
		private static final long serialVersionUID = -3644628680465212593L;

		ReadException(String format, Object... values)
		{
			super(format.formatted(values));
		}
	}
	
	private static class FormatException extends Exception
	{
		private static final long serialVersionUID = 6641514199637112262L;

		FormatException(String format, Object... values)
		{
			super(format.formatted(values));
		}
	}

	private static class Tester
	{
		private static void processCommandLineArgs(String[] args)
		{
			List<File> files = new ArrayList<>();
			int verboseLevel = Integer.MAX_VALUE;
			boolean showGUI = false;
			
			for (int i=0; i<args.length; i++)
			{
				if (args[i].equalsIgnoreCase("-gui"))
					showGUI = true;
					
				if (args[i].equalsIgnoreCase("-file") && i+1<args.length)
				{
					File file = new File(args[i+1]);
					if (file.isFile())
					{
						i++;
						files.add(file);
						verboseLevel = Math.min(verboseLevel, VERBOSE_LEVEL_Blocks_Only);
					}
				}
				
				if (args[i].equalsIgnoreCase("-folder") && i+1<args.length)
				{
					File folder = new File(args[i+1]);
					if (folder.isDirectory())
					{
						i++;
						File[] filesInFolder = folder.listFiles(file -> {
							if (!file.isFile()) return false;
							String name = file.getName();
							return (name.startsWith("save") && name.endsWith(".hg"));
						});
						files.addAll(Arrays.asList(filesInFolder));
						verboseLevel = Math.min(verboseLevel, VERBOSE_LEVEL_Files_Only);
					}
				}
			}
			
			if (!showGUI)
			{
				int verboseLevel_ = verboseLevel;
				files.forEach(file -> decodeFile(file, verboseLevel_));
			}
			else
			{
				try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
				catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
				new Tester(files);
			}
		}

		private final StandardMainWindow mainWindow;

		public Tester(List<File> files)
		{
			mainWindow = new StandardMainWindow("SaveGameDecoder - Tester");
			
			JPanel contentPane = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			
			for (File file : files)
			{
				c.gridwidth = 1;
				
				contentPane.add(new JLabel("%s :  ".formatted(file.getName())), c);
				
				contentPane.add(Gui.createButton("Show Raw Content", e -> {
					byte[] bytes;
					try { bytes = Files.readAllBytes(file.toPath()); }
					catch (IOException ex) { ex.printStackTrace(); return; }
					SaveViewer.showBytes(mainWindow, "Raw Content of %s".formatted(file.getName()), bytes, ModalityType.MODELESS);
				}), c);
				
				contentPane.add(Gui.createButton("Decode & Show Result", e -> {
					byte[] decodedBytes = decodeFile(file, VERBOSE_LEVEL_Blocks_Only);
					SaveViewer.showBytes(mainWindow, "Decoded Content of %s".formatted(file.getName()), decodedBytes, ModalityType.MODELESS);
				}), c);
				
				contentPane.add(Gui.createButton("Decode in separate Thread", e -> {
					new Thread(() -> decodeFile(file, VERBOSE_LEVEL_Blocks_Only)).start();
				}), c);
				
				c.gridwidth = GridBagConstraints.REMAINDER;
				
				contentPane.add(Gui.createButton("Read & Parse JSON", e -> {
					byte[] decodedBytes = decodeFile(file, VERBOSE_LEVEL_Blocks_Only);
					if (decodedBytes!=null)
					{
						String content = new String(decodedBytes, StandardCharsets.UTF_8);
						JSON_Parser.parse(content,new FactoryForExtras(),null);
					}
				}), c);
			}
			
			mainWindow.startGUI(contentPane);
		}
	
	}
}
