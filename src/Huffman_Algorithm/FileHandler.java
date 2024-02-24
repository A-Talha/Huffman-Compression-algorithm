package Huffman_Algorithm;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

public class FileHandler {
    private static final int BUFFER_SIZE = 1024 * 1024 * 315;
    private static int numberOfBytes = 2;

    public void compress(String filePath, int n) {
        numberOfBytes = n;
        Map<Symbol, Integer> frequencies = frequencyTable(filePath);

        HuffmanNode root = buildHuffmanTree(frequencies);

        // change Bitset to Byte array
        Map<Symbol, MyBitSet> huffmanCodeTable = new HashMap<>();
        // print the huffman code table
        buildHuffmanCodeTable(root, huffmanCodeTable, new MyBitSet());
        // calculate the size of the compressed file in KB and print it
        double compressedFileSize = calculateCompressedFileSize(frequencies, huffmanCodeTable);
        //add header size to the compressed file size
        compressedFileSize += 4 + ((numberOfBytes + 4 + 4)*huffmanCodeTable.size());
        //get original file size
        File file = new File(filePath);
        double originalFileSize = file.length();
        //calculate compression ratio
        double compressionRatio = compressedFileSize/originalFileSize;
        System.out.println("Compression ratio: " + compressionRatio);

        String newFilePath = createFilePath(filePath);
        writeCompressedFile(filePath, newFilePath, huffmanCodeTable);
    }

    public void decompress(String filePath) {
        String extractedFilePath = extractFilepath(filePath);

        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(filePath))) {
            int numberOfSymbols = readInt(inputStream);

            // Read the Huffman code table
            Map<Symbol, MyBitSet> huffmanCodeTable = readHuffmanCodeTable(inputStream, numberOfSymbols);

            // Initialize the priority queue
            PriorityQueue<DecompressedNode> Q = new PriorityQueue<>();
            huffmanCodeTable.forEach((symbol, bitSet) -> Q.add(new DecompressedNode(symbol, bitSet)));

            // Build the reverse Huffman tree
            DecompressedNode root = new DecompressedNode(null, new MyBitSet());
            buildReverseHuffmanTree(Q, root);

            // Decompress the file
            decompressFile(inputStream, root, extractedFilePath);
        } catch (IOException e) {
            e.printStackTrace(); // Handle or log the exception appropriately
        }
    }

    private int readInt(BufferedInputStream inputStream) throws IOException {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            int byteValue = inputStream.read();
            if (byteValue == -1) {
                // Handle end of stream or other error
                break;
            }
            result = (result << 8) | byteValue;
        }
        return result;
    }

    private Map<Symbol, MyBitSet> readHuffmanCodeTable(BufferedInputStream inputStream, int numberOfSymbols)
            throws IOException {
        Map<Symbol, MyBitSet> huffmanCodeTable = new HashMap<>();

        for (int i = 0; i < numberOfSymbols; i++) {
            // Read the symbol
            byte[] symbolBytes = new byte[numberOfBytes];
            inputStream.read(symbolBytes);
            Symbol symbol = new Symbol(symbolBytes);

            // Read the length of Huffman code
            int huffmanCodeLength = readInt(inputStream);

            // Read the Huffman code
            byte[] huffmanCodeBytes = new byte[(int) Math.ceil(huffmanCodeLength / 8.0)];
            inputStream.read(huffmanCodeBytes);
            MyBitSet huffmanCode = byteArrayToMyBitSet(huffmanCodeBytes, huffmanCodeLength);
            huffmanCodeTable.put(symbol, huffmanCode);
        }

        return huffmanCodeTable;
    }

    private MyBitSet byteArrayToMyBitSet(byte[] huffmanCodeBytes, int huffmanCodeLength) {
        MyBitSet huffmanCode = new MyBitSet();
        for (int i = 0; i < huffmanCodeLength; i++) {
            int byteIndex = i / 8;
            int bitIndex = i % 8;
            boolean bit = (huffmanCodeBytes[byteIndex] & (1 << bitIndex)) != 0;
            huffmanCode.set(i, bit);
        }
        return huffmanCode;
    }

    private void decompressFile(BufferedInputStream inputStream, DecompressedNode root, String extractedFilePath)
            throws IOException {
        try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(extractedFilePath))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            DecompressedNode current = root;

            while ((bytesRead = inputStream.read(buffer)) != -1) {

                int i = 0;
                int totalBits = bytesRead * 8;
                current = root;
                MyBitSet symbolBits = new MyBitSet();

                while (i < totalBits) {
                    if (current.left == null && current.right == null) {
                        if (current.code.toLong() == symbolBits.toLong()) {
                            outputStream.write(current.symbol.value);
                            current = root;
                            symbolBits = new MyBitSet();
                        } else {
                            symbolBits = new MyBitSet();
                            current = root;
                        }
                    }

                    boolean bit = (buffer[i / 8] & (1 << (i % 8))) != 0;
                    if (bit) {
                        symbolBits.addOne();
                        current = current.right;
                    } else {
                        symbolBits.addZero();
                        current = current.left;
                    }
                    i++;
                }
            }
        }
    }

    private MyBitSet byteToMyBitSet(byte currentByte) {
        MyBitSet currentByteBits = new MyBitSet();
        for (int j = 0; j < 8; j++) {
            boolean bit = (currentByte & (1 << j)) != 0;
            if (bit) {
                currentByteBits.addOne();
            } else {
                currentByteBits.addZero();
            }
        }
        return currentByteBits;
    }

    private void buildReverseHuffmanTree(PriorityQueue<DecompressedNode> Q, DecompressedNode root) {
        while (!Q.isEmpty()) {
            addNode(Q.poll(), root);
        }
    }

    private void addNode(DecompressedNode current, DecompressedNode root) {
        if (current.code.length() != root.code.length()) {
            if (current.code.get(root.code.length())) {
                if (root.right == null) {
                    // Create a deep copy of the bitset
                    DecompressedNode node = new DecompressedNode(null, new MyBitSet(root.code.toLong(), root.code.length()));
                    node.code.addOne();
                    root.right = node;
                }
                addNode(current, root.right);
            } else {
                if (root.left == null) {
                    // Create a deep copy of the bitset
                    DecompressedNode node = new DecompressedNode(null, new MyBitSet(root.code.toLong(), root.code.length()));
                    node.code.addZero();
                    root.left = node;
                }
                addNode(current, root.left);
            }
        } else {
            root.symbol = current.symbol;
            root.code = current.code;
        }
    }


    private Map<Symbol, Integer> frequencyTable(String filePath) {
        Map<Symbol, Integer> frequencies = new HashMap<>();

        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(filePath))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i += numberOfBytes) {
                    byte[] currentSymbolBytes = new byte[numberOfBytes];
                    for (int j = 0; j < numberOfBytes && i + j < bytesRead; j++) {
                        currentSymbolBytes[j] = buffer[i + j];
                    }
                    Symbol currentSymbol = new Symbol(currentSymbolBytes);
                    frequencies.put(currentSymbol, frequencies.getOrDefault(currentSymbol, 0) + 1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); // Handle or log the exception appropriately
        }

        return frequencies;
    }

    private HuffmanNode buildHuffmanTree(Map<Symbol, Integer> frequencies) {
        PriorityQueue<HuffmanNode> Q = new PriorityQueue<>();
        frequencies.forEach((symbol, frequency) -> Q.add(new HuffmanNode(symbol, frequency)));

        while (Q.size() > 1) {
            HuffmanNode left = Q.poll();
            HuffmanNode right = Q.poll();
            HuffmanNode parent = new HuffmanNode(null, left.frequency + right.frequency);
            parent.setLeft(left);
            parent.setRight(right);
            Q.add(parent);
        }

        return Q.poll();
    }

    private void buildHuffmanCodeTable(HuffmanNode root, Map<Symbol, MyBitSet> huffmanCodeTable, MyBitSet code) {
        if (root.left == null && root.right == null) {
            huffmanCodeTable.put(root.symbol, code);
            return;
        }

        if (root.left != null) {
            MyBitSet leftCode = new MyBitSet(code);
            leftCode.addZero();
            buildHuffmanCodeTable(root.left, huffmanCodeTable, leftCode);
        }

        if (root.right != null) {
            MyBitSet rightCode = new MyBitSet(code);
            rightCode.addOne();
            buildHuffmanCodeTable(root.right, huffmanCodeTable, rightCode);
        }
    }

    private double calculateCompressedFileSize(Map<Symbol, Integer> frequencies, Map<Symbol, MyBitSet> huffmanCodeTable) {
        double compressedFileSize = 0;
        // Calculate the size of the compressed file
        for (Map.Entry<Symbol, Integer> entry : frequencies.entrySet()) {
            Symbol symbol = entry.getKey();
            int frequency = entry.getValue();
            MyBitSet huffmanCode = huffmanCodeTable.get(symbol);
            compressedFileSize += frequency * huffmanCode.length();
        }

        // Convert the size from bits to bytes
        compressedFileSize = Math.ceil(compressedFileSize / 8);

        return compressedFileSize;
    }

    private void writeCompressedFile(String filePath, String newFilePath, Map<Symbol, MyBitSet> huffmanCodeTable) {
        try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(newFilePath))) {
            // Write the number of symbols
            int size = huffmanCodeTable.size();

            // Write the size using four bytes in big-endian order
            outputStream.write((size >> 24) & 0xFF);
            outputStream.write((size >> 16) & 0xFF);
            outputStream.write((size >> 8) & 0xFF);
            outputStream.write(size & 0xFF);

            // Write the Huffman code table
            for (Map.Entry<Symbol, MyBitSet> entry : huffmanCodeTable.entrySet()) {
                Symbol symbol = entry.getKey();
                MyBitSet huffmanCode = entry.getValue();

                // Write the symbol
                outputStream.write(symbol.value);

                // Write the length of Huffman code as a byte
                int huffmanCodeLength = huffmanCode.length();

                // Write the Huffman code length using four bytes in big-endian order
                outputStream.write((huffmanCodeLength >> 24) & 0xFF);
                outputStream.write((huffmanCodeLength >> 16) & 0xFF);
                outputStream.write((huffmanCodeLength >> 8) & 0xFF);
                outputStream.write(huffmanCodeLength & 0xFF);

                // Write the Huffman code
                outputStream.write(huffmanCode.toByteArray());
            }

            // Write the compressed file
            try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(filePath))) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    StringBuilder bitStringBuilder = new StringBuilder();
                    for (int i = 0; i < bytesRead; i += numberOfBytes) {
                        byte[] currentSymbolBytes = new byte[numberOfBytes];
                        for (int j = 0; j < numberOfBytes && i + j < bytesRead; j++) {
                            currentSymbolBytes[j] = buffer[i + j];
                        }
                        Symbol currentSymbol = new Symbol(currentSymbolBytes);
                        MyBitSet huffmanCode = huffmanCodeTable.get(currentSymbol);
                        // add huffmanCode to bit by bit to the bitStringBuilder
                        for (int j = 0; j < huffmanCode.length(); j++) {
                            bitStringBuilder.append(huffmanCode.get(j) ? '1' : '0');
                        }
                    }
                    int bitStringLength = bitStringBuilder.length();
                    int bitStringIndex = 0;
                    int byteLength = (int) Math.ceil(bitStringLength / 8.0);
                    byte[] bitStringBytes = new byte[byteLength];

                    for (int i = 0; i < byteLength; i++) {
                        byte currentByte = 0;

                        // Set every bit in the byte using bit operations
                        for (int j = 0; j < 8; j++) {
                            if (bitStringIndex < bitStringLength && bitStringBuilder.charAt(bitStringIndex) == '1') {
                                currentByte |= (byte) (1 << j);
                            }
                            bitStringIndex++;
                        }

                        bitStringBytes[i] = currentByte;
                    }

                    outputStream.write(bitStringBytes);


                }
            } catch (IOException e) {
                e.printStackTrace(); // Handle or log the exception appropriately
            }
        } catch (IOException e) {
            e.printStackTrace(); // Handle or log the exception appropriately
        }
    }

    private String createFilePath(String filePath) {
        // Extract the file name with extension from the input file path
        String fileNameWithExtension = filePath.substring(filePath.lastIndexOf('\\') + 1);
        String newFileName = "20010888." + numberOfBytes + "." + fileNameWithExtension + ".hc";
        // create newFilePath
        return filePath.substring(0, filePath.lastIndexOf('\\') + 1) + newFileName;
    }
    private String extractFilepath(String filePath) {
        String fileNameWithExtension = filePath.substring(filePath.lastIndexOf('\\') + 1);
        //extract the original file name with extension and n
        //file name format: 20010888.<n>.<original_file_name>.extension.hc
        String[] fileNameParts = fileNameWithExtension.split("\\.");
        String newFileName = "extracted." + fileNameWithExtension.substring(0, fileNameWithExtension.length()-3);
        numberOfBytes = Integer.parseInt(fileNameParts[1]);
        String newFilePath = filePath.substring(0,filePath.lastIndexOf('\\') + 1) + newFileName;
        return newFilePath;
    }



    private static class HuffmanNode implements Comparable<HuffmanNode> {
        Symbol symbol;
        int frequency;
        HuffmanNode left;
        HuffmanNode right;

        public HuffmanNode(Symbol symbol, int frequency) {
            this.symbol = symbol;
            this.frequency = frequency;
            this.left = null;
            this.right = null;
        }

        public void setLeft(HuffmanNode left) {
            this.left = left;
        }

        public void setRight(HuffmanNode right) {
            this.right = right;
        }

        @Override
        public int compareTo(HuffmanNode other) {
            return Integer.compare(this.frequency, other.frequency);
        }
    }

    private static class Symbol implements Comparable<Symbol> {
        byte[] value;

        public Symbol(byte[] value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Symbol symbol = (Symbol) obj;
            return Arrays.equals(value, symbol.value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }

        @Override
        public int compareTo(Symbol other) {
            return Arrays.compare(this.value, other.value);
        }
    }

    private static class MyBitSet {
        long set;
        int length;

        public MyBitSet() {
            this.set = 0;
            this.length = 0;
        }

        public MyBitSet(MyBitSet other) {
            this.set = other.toLong();
            this.length = other.length;
        }

        public MyBitSet(long aLong, int length) {
            this.set = aLong;
            this.length = length;
        }

        // implement set similar to BitSet
        public void set(int index, boolean value) {
            if (value) {
                set |= (1L << index);
            } else {
                set &= ~(1L << index);
            }
            length = Math.max(length, index + 1);
        }

        public boolean get(int index) {
            return (set & (1L << index)) != 0;
        }

        public int length() {
            return length;
        }


        // add one 0 to the end of the bitset
        public void addZero() {
            set(length, false);
        }

        // add one 1 to the end of the bitset
        public void addOne() {
            set(length, true);
        }


        // toByteArray similar to BitSet
        public byte[] toByteArray() {
            byte[] bytes = new byte[(int) Math.ceil(length / 8.0)];
            for (int i = 0; i < length; i++) {
                if (get(i)) {
                    bytes[i / 8] |= (byte) (1 << (i % 8));
                }
            }
            return bytes;
        }

        public long toLong() {
            return set;
        }
    }
    private static class MyBitSetList {
        List<Long> set;
        int j;

        public MyBitSetList() {
            this.set = new ArrayList<>();
            this.set.add(0L);
            this.j = 0;
        }
        public void add(byte b) {
            int i = 0;
            while (j < set.size() * 64  && i < 8) {
                int index = j%64;
                Long temp = set.get(set.size() - 1);
                long bit = (b & (1 << i)) != 0 ? 1L : 0L;
                temp |= (bit << index);
                set.set(set.size() - 1, temp);
                j++;
                i++;
            }
            if (i < 8) {
                this.set.add(0L);
                while (j < set.size() * 64  && i < 8) {
                    int index = j%64;
                    Long temp = set.get(set.size() - 1);
                    long bit = (b & (1 << i)) != 0 ? 1L : 0L;
                    temp |= (bit << index);
                    set.set(set.size() - 1, temp);
                    j++;
                    i++;
                }
            }
        }

        public void add(MyBitSet other) {
            int i = 0;
            while (j < set.size() * 64  && i < other.length()) {
                int index = j%64;
                Long temp = set.get(set.size() - 1);
                long bit = other.get(i) ? 1L : 0L;
                temp |= (bit << index);
                set.set(set.size() - 1, temp);
                j++;
                i++;
            }
            if (i < other.length()) {
                this.set.add(0L);
                while (j < set.size() * 64  && i < other.length()) {
                    int index = j%64;
                    Long temp = set.get(set.size() - 1);
                    long bit = other.get(i) ? 1L : 0L;
                    temp |= (bit << index);
                    set.set(set.size() - 1, temp);
                    j++;
                    i++;
                }
            }
        }

        // toByteArray similar to BitSet
        public byte[] toByteArray() {
            int size = this.set.size() * 8; // Each Long has 8 bytes
            byte[] bytes = new byte[size];

            int byteIndex = 0;
            for (Long currentLong : this.set) {
                byte[] currentLongBytes = LongToByteArray(currentLong);
                for (byte currentLongByte : currentLongBytes) {
                    bytes[byteIndex++] = currentLongByte;
                }
            }

            return bytes;
        }

        private byte[] LongToByteArray(long value) {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(value);
            return buffer.array();
        }

        public boolean isEmpty() {
            return set.size() == 1 && set.get(0) == 0;
        }

        public int length() {
            return j;
        }

        public boolean get(int i) {
            int index = i/64;
            int bitIndex = i%64;
            return (set.get(index) & (1L << bitIndex)) != 0;
        }

        public void clear(int i, int i1) {
            int index = i/64;
            int bitIndex = i%64;
            int index1 = i1/64;
            int bitIndex1 = i1%64;
            if (index == index1) {
                long temp = set.get(index);
                long mask = (1L << (bitIndex1 - bitIndex)) - 1;
                mask = ~mask;
                temp &= mask;
                if (temp == 0)
                    set.remove(index);
                else
                    set.set(index, temp);
            }
            else {
                long temp = set.get(index);
                long mask = (1L << (64 - bitIndex)) - 1;
                mask = ~mask;
                temp &= mask;
                if (temp == 0)
                    set.remove(index);
                else
                    set.set(index, temp);
                for (int k = index + 1; k < index1; k++) {
                    set.remove(k);
                }
                temp = set.get(index1);
                mask = (1L << bitIndex1) - 1;
                mask = ~mask;
                temp &= mask;
                if (temp == 0)
                    set.remove(index1);
                else
                    set.set(index1, temp);
            }
        }
    }
    private static class DecompressedNode implements Comparable<DecompressedNode> {
        Symbol symbol;
        MyBitSet code;
        DecompressedNode left;
        DecompressedNode right;

        public DecompressedNode(Symbol symbol, MyBitSet code) {
            this.symbol = symbol;
            this.left = null;
            this.right = null;
            this.code = code;
        }

        public void setLeft(DecompressedNode left) {
            this.left = left;
        }

        public void setRight(DecompressedNode right) {
            this.right = right;
        }

        @Override
        public int compareTo(DecompressedNode other) {
            return Integer.compare(this.code.length(), other.code.length());
        }
    }


}
