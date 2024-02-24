# Huffman Compression Algorithm

## Overview
This Java application implements Huffman compression algorithm for file compression and decompression.

## Usage
### Compression
To compress a file, use the following command:
```bash
java Huffman_Algorithm.FileHandler compress [file_path] [number_of_bytes]
```
- `[file_path]`: Path to the file you want to compress.
- `[number_of_bytes]`: Number of bytes used to represent each symbol (e.g., 1 for ASCII characters, 2 for Unicode characters).

### Decompression
To decompress a file, use the following command:
```bash
java Huffman_Algorithm.FileHandler decompress [compressed_file_path]
```
- `[compressed_file_path]`: Path to the compressed file you want to decompress.

## Example
### Compression
```bash
java Huffman_Algorithm.FileHandler compress input.txt 1
```
This compresses the file `input.txt` using 1 byte for each symbol.

### Decompression
```bash
java Huffman_Algorithm.FileHandler decompress input.txt.hc
```
This decompresses the file `input.txt.hc`.

## Notes
- The compressed file will be saved with the extension `.hc` appended to the original file name.

Feel free to reach out if you have any questions or encounter issues.
