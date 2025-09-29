# QRCode
Class to Generate a QR code in the command line from a given URL. Medium strength (15%) error correction. Works fairly well up to a version 3 QR Code, which allows for 42 input bytes. Anything more than 42 input bytes will throw an error when it tries to calculate the error correction polynomial.


To use, download all files into a folder and cd into that folder

```java
javac Generator.java
```
Your command line will create Generator.class and UglyStuff.class files
```java
java Generator
```
You will be prompted with
```java
Enter your URL: 
```
Once you enter your URL, a QR Code will be printed in the command line that you can take a screenshot of.