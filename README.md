# QRCode
Class to Generate a QR code in the command line from a given URL. Medium strength (15%) error correction. Works fairly well up to a version 6 QR Code, which allows for 106 input bytes. Anything more than 106 input bytes will terminate early.


To use, download all files into a folder and cd into that folder

```Bash
javac Generator.java
```
Your command line will create Generator.class and UglyStuff.class files
```Bash
java Generator
```
You will be prompted with
```Bash
Enter your URL: 
```
Once you enter your URL, a QR Code will be printed in the command line that you can take a screenshot of.