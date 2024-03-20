# DCT Image Compression with Progressive Decoding

This repository contains a Java implementation of a Discrete Cosine Transform (DCT) based image compression algorithm with support for progressive decoding simulation. The algorithm mimics the compression techniques used in standard compression algorithms like JPEG and MPEG.

## Features

- DCT-based coder-decoder for compressing images
- Support for baseline and progressive delivery modes
- Simulation of decoding using both baseline and progressive modes with latency control

## Usage

To use the program, compile and run the Java file with the following command:

```
javac MyProgram.java
java MyProgram InputImage quantizationLevel DeliveryMode Latency
```

- `InputImage`: Path to the input image file (RGB format).
- `quantizationLevel`: A factor that affects compression (0 to 7).
- `DeliveryMode`: 1 for baseline, 2 for progressive using spectral selection, 3 for progressive using successive bit approximation.
- `Latency`: Latency in milliseconds for simulating data delivery during decoding.

## Example Invocations

1. Baseline mode with no quantization and no latency:
```
java MyProgram Example.rgb 0 1 0
```

2. Baseline mode with quantization level 3 and latency of 100ms:
```
java MyProgram Example.rgb 3 1 100
```

3. Progressive mode using spectral selection with quantization level 1 and latency of 100ms:
```
java MyProgram Example.rgb 1 2 100
```

## Implementation Details

- **Encoder**: Breaks the RGB image into 8x8 blocks, performs DCT on each block, quantizes the coefficients, and outputs the compressed data.
- **Decoder**: Dequantizes the coefficients, performs inverse DCT, and reconstructs the image.
- **Progressive Decoding**: Simulates sequential, spectral selection, and successive bit approximation modes with latency control.

## Requirements

- Java Development Kit (JDK)
- Java IDE (optional)
