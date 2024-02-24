package Huffman_Algorithm;


public class main {
    public static void main(String[] args) {
        if (args.length == 2 || args.length == 3) {
            String option = args[0];
            String FilePath = args[1];
            FileHandler fileHandler = new FileHandler();
            if (args.length == 3) {
                int n = Integer.parseInt(args[2]);
                //start calculating time
                long startTime = System.currentTimeMillis();
                fileHandler.compress(FilePath, n);
                //end calculating time
                long endTime = System.currentTimeMillis();
                System.out.println("Time taken: " + (endTime - startTime) + " milliseconds");
            }
            else {
                long startTime = System.currentTimeMillis();
                fileHandler.decompress(FilePath);
                long endTime = System.currentTimeMillis();
                System.out.println("Time taken: " + (endTime - startTime) + " milliseconds");
            }
        }
        else {
            System.out.println("Usage: java huffman_20010888 <option> <absolute_path_to_input_file> <n>");
            System.out.println("option: -c for compression, -d for decompression");
            System.out.println("n: number of bytes to be used for each symbol");
            System.exit(1);
        }
    }
}
